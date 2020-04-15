#!/bin/bash

#apt-get -y install vim curl mc rabbitmq-server
rabbitmq-plugins enable rabbitmq_management
rabbitmqctl add_user $1 $2
rabbitmqctl set_user_tags $1 administrator
rabbitmqctl set_permissions -p / $1 ".*" ".*" ".*"
echo $1 $2 > /home/lex/rabbit