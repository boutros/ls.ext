
branch_import:
  cmd.run:
    - name: docker run
              -e "MYSQLPASS={{ pillar['koha']['adminpass'] }}"
              -e "MYSQLUSER={{ pillar['koha']['adminuser'] }}" 
              -e "KOHAINSTANCE={{ pillar['koha']['instance'] }}"
              -e "MYSQLHOST=db"
              -e "BCRYPT=true"
              -v "{{ pillar['migration-data-folder'] }}:/migration/data"
              --link koha_mysql_container:db 
              deichman/migration:{{ pillar['migration']['image-tag'] }}
              make --file /migration/sh/Makefile import_branches
    - failhard: True