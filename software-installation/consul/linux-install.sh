#!/bin/bash

apt-get update
apt-get install -y cifs-utils
useradd consul
echo consul_test:consul | chpasswd

conf_dir=/etc/consul.d/
data_dir=/var/consul
source=//share/soft/consul/linux
tmp_mnt=/tmp/consul/
if [ ! -d "$conf_dir" ]; then
  mkdir $conf_dir
fi

if [ ! -d "$data_dir" ]; then
  mkdir $data_dir
fi
chown consul: $data_dir

if [ ! -d "$tmp_mnt" ]; then
  mkdir $tmp_mnt
fi

mount -t cifs -ro user=lex,password=Lexroot12qwe $source $tmp_mnt
cp $tmp_mnt/client-config.json $conf_dir
cp $tmp_mnt/consul /usr/local/bin
cp $tmp_mnt/consul.service /etc/systemd/system/
umount $tmp_mnt
chmod +x /usr/local/bin/consul
systemctl enable consul.service
systemctl start consul.service
