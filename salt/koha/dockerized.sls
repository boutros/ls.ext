##########
# KOHA LOGS VOLUME
##########

koha_logs_volume_installed:
  docker.installed:
    - name: koha_logs_volume
    - image: busybox
    - volumes:
      - /var/log

koha_logs_volume_run_once:
  docker.running:
    - name: koha_logs_volume
    - volumes: /var/log
    - check_is_running: False
    - require:
      - docker: koha_logs_volume_installed

##########
# KOHA DOCKER CONTAINER
##########

apache2:
  service.dead

# Check if pulled koha image is newer than running container, if so: stop and delete running koha container
koha_container_stop_if_old:
  cmd.run:
    - name: docker stop koha_container || true # Next line baffling? http://jinja.pocoo.org/docs/dev/templates/#escaping - Note: egrep-expression must yield single line
    - unless: docker inspect --format "{{ '{{' }} .Image {{ '}}' }}" koha_container | grep $(docker images | egrep "digibib/koha[[:space:]]+{{ pillar['koha']['koha-image-tag'] }}[[:space:]]+" | awk '{ print $3 }')
    - require:
      - docker: mysql_docker_image

koha_container_remove_if_old:
  cmd.run:
    - name: docker rm koha_container || true
    - unless: docker inspect --format "{{ '{{' }} .Image {{ '}}' }}" koha_container | grep $(docker images | egrep "digibib/koha[[:space:]]+{{ pillar['koha']['koha-image-tag'] }}[[:space:]]+" | awk '{ print $3 }')
    - require:
      - cmd: koha_container_stop_if_old

koha_container_installed:
  docker.installed:
    - name: koha_container
    - image: digibib/koha:{{ pillar['koha']['koha-image-tag'] }}
    - environment:
      - "KOHA_ADMINPASS": "{{ pillar['koha']['adminpass'] }}"
      - "KOHA_ADMINUSER": "{{ pillar['koha']['adminuser'] }}"
      - "KOHA_INSTANCE": "{{ pillar['koha']['instance'] }}"
    - ports:
      - "8080/tcp"
      - "8081/tcp"
    - volumes:
      - /var/log
    - require:
      - docker: koha_docker_image
      - docker: koha_logs_volume_installed

koha_container_running:
  docker.running:
    - container: koha_container
    - port_bindings:
        "8080/tcp":
            HostIp: "0.0.0.0"
            HostPort: "8080"
        "8081/tcp":
            HostIp: "0.0.0.0"
            HostPort: "8081"
    - check_is_running:
      - "koha_mysql_container"
    - links:
        koha_mysql_container: db
    - volumes_from:
      - "koha_logs_volume"
    - watch:
      - docker: koha_container_installed
      - docker: koha_mysql_container_running
      - docker: koha_logs_volume_run_once
