
{% set container = 'fuseki_container' %}
{% set repo = 'fisch42' %}
{% set image = 'fuseki' %}
{% set tag = 'latest' %}
{% set ports = ["3030/tcp"] %}
{% set environment = {} %}
{% set port_bindings = {'3030/tcp': { 'HostIp': pillar['redef']['fuseki']['binding'], 'HostPort': pillar['redef']['fuseki']['port'] } } %}
{% set data_volume = { 'container': 'fuseki_data', 'image': 'busybox', 'tag': 'latest', 'volumes': ['/data'] } %}

{% include 'docker.sls-fragment' %}