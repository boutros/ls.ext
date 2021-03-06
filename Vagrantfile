# -*- mode: ruby -*-
# vi: set ft=ruby :
require 'fileutils'

SALT_VERSION="2015.5.3+ds-1trusty1"

vagrant_root = File.dirname(__FILE__)

# Template of Koha config copied if not existing
koha_pillar_example = "#{vagrant_root}/pillar/koha/admin.sls.example"
koha_pillar_example_prev = koha_pillar_example + "_prev"
pillar_file = koha_pillar_example.sub(/\.example$/, '')
if !File.file?(pillar_file) || FileUtils.compare_file(pillar_file, koha_pillar_example_prev)
  puts "Note! Copying #{pillar_file} from #{koha_pillar_example} ..."
  FileUtils.cp(koha_pillar_example, pillar_file)
else
  puts "Note! #{pillar_file} is in sync with original."
end
if !FileUtils.compare_file(pillar_file, koha_pillar_example)
  puts "Note: You are running a customized #{pillar_file}."
end

# Migration example config
migration_pillar_example_file= "#{vagrant_root}/pillar/migration/admin.sls.example"
migration_pillar_file = migration_pillar_example_file.sub(/\.example$/, '')
if !File.file?(migration_pillar_file)
  raise "ERROR: You need to create a valid #{migration_pillar_file} based on #{migration_pillar_example_file}"
end

Vagrant.configure(2) do |config|

  # **** vm-ship - Docker container ship ****

  ship_name = (ENV['LSDEVMODE'] || "vm") + "-ship" # Set LSDEVMODE to 'dev' or 'build'
  config.vm.define ship_name do |config|
    # https://vagrantcloud.com/ubuntu/trusty64
    config.vm.box = "ubuntu/trusty64"
    config.vm.hostname = ship_name

    # http://fgrehm.viewdocs.io/vagrant-cachier
    if Vagrant.has_plugin?("vagrant-cachier")
      config.cache.scope = :box
    end

    config.vm.provider "virtualbox" do |vb|
      vb.memory = 5120
      vb.cpus = 4
    end

    config.vm.provision "shell" do |s|
      s.privileged = false
      s.inline = "sudo sed -i '/tty/!s/mesg n/tty -s \\&\\& mesg n/' /root/.profile"
    end

    config.vm.network "private_network", ip: "192.168.50.12"
    # Sync folders salt and pillar in virtualboxes
    config.vm.synced_folder "salt", "/srv/salt"
    config.vm.synced_folder "pillar", "/srv/pillar"

    if ENV['LSDEVMODE'] == 'vm' || ENV['LSDEVMODE'] == 'dev'
      # sync node app source trees. Use NFS to enable instant reload
      if ENV['MOUNT_WITH_NFS']
        config.vm.synced_folder "redef/catalinker/server", "/mnt/catalinker_server", type: "nfs"
        config.vm.synced_folder "redef/catalinker/client", "/mnt/catalinker_client", type: "nfs"
        config.vm.synced_folder "redef/catalinker/public", "/mnt/catalinker_public", type: "nfs"

        config.vm.synced_folder "redef/patron-client/src", "/mnt/patron-client_src", type: "nfs"
        config.vm.synced_folder "redef/patron-client/test", "/mnt/patron-client_test", type: "nfs"
      else
        config.vm.synced_folder "redef/catalinker/server", "/mnt/catalinker_server"
        config.vm.synced_folder "redef/catalinker/client", "/mnt/catalinker_client"
        config.vm.synced_folder "redef/catalinker/public", "/mnt/catalinker_public"

        config.vm.synced_folder "redef/patron-client/src", "/mnt/patron-client_src"
        config.vm.synced_folder "redef/patron-client/test", "/mnt/patron-client_test"
      end
    end

    config.vm.provision "shell", inline: "sudo add-apt-repository -y ppa:saltstack/salt && \
      sudo apt-get update && \
      sudo apt-get install -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold salt-minion=\"#{SALT_VERSION}\" salt-master=\"#{SALT_VERSION}\""

    config.vm.provision "shell", path: "docker_install.sh"
    config.vm.provision "shell", path: "pip_install.sh"
    config.vm.provision "shell", inline: "sudo pip install pyopenssl ndg-httpsclient pyasn1 docker-compose==1.6.0"

    config.vm.provision "shell", path: "ssh/generate_keys.sh"
    config.vm.provision "shell", path: "ssh/accept_keys.sh"

    if ENV['LSDEVMODE']
      # this should be rewritten to salt-states?
      config.vm.provision "shell", path: "redef/upgrade_once.sh"
      config.vm.provision "shell", path: "redef/install_jdk.sh"
      config.vm.provision "shell", path: "redef/install_git-up.sh"
    end

    if ENV['LSDEVMODE'] && ENV['LSDEVMODE'] == 'dev'
      config.vm.provision "shell", path: "redef/set_gradle_daemon.sh"
    end

    config.vm.provision "shell", inline: "sudo BUILD_TAG=\"#{ENV['BUILD_TAG']}\" \
      GITREF=\"#{ENV['GITREF']}\" LSDEVMODE=\"#{ENV['LSDEVMODE']}\" \
      /vagrant/docker-compose/docker-compose.sh"

    config.vm.provision :salt do |salt|
      salt.bootstrap_options = "-F -c /tmp -P" # Vagrant Issues #6011, #6029
      salt.minion_config = "salt/minion"
      salt.run_highstate = true
      salt.verbose = true
      salt.pillar_data
      salt.pillar({'GITREF' => ENV['GITREF']}) if ENV['GITREF']
    end
  end

end
