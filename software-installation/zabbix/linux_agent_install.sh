wget https://repo.zabbix.com/zabbix/4.0/ubuntu/pool/main/z/zabbix-release/zabbix-release_4.0-2+xenial_all.deb
dpkg -i zabbix-release_4.0-2+xenial_all.deb
apt update
apt-get install -y zabbix-agent
touch /etc/zabbix/zabbix_agentd.conf
echo -e "PidFile=/var/run/zabbix/zabbix_agentd.pid \nLogFile=/var/log/zabbix/zabbix_agentd.log \nLogFileSize=50 \nServer=zabbix.bit-erp.loc \nServerActive=zabbix.bit-erp.loc \nEnableRemoteCommands=1 \nLogRemoteCommands=1 \nHostMetadataItem=system.uname \nHostMetadata=Infrastructure_linux \n# Hostname=" > /etc/zabbix/zabbix_agentd.conf
systemctl enable zabbix-agent
systemctl restart zabbix-agent.service
