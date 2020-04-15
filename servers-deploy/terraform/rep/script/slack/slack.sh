#!/bin/bash

#url='https://hooks.slack.com/services/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'
url='https://hooks.slack.com/services/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'

username='terraform'

to="$1"
subject="$2" 
to_chanel="G79LPJK0W"

recoversub='^RECOVER(Y|ED)?$'
if [[ "$subject" =~ ${recoversub} ]]; then
	emoji=':smile:'
elif [ "$subject" == 'PROBLEM' ]; then
	emoji=':frowning:'
else
	emoji=':ghost:'
fi



if test "$6" = "RabbitMQ"
then
m="Для подключения к серверу используеться webui \n\`*ip*\` : _$3_ \n \`*port*\` : 15672 \n Для подключения очереди amqp \n\`*ip*\` : _$3_ \n \`*port*\` : 5672 \n \`*Пользователь*\` : $4\n \`*Пароль*\` : $5  \n $7"
fi

if test "$6" = "WinDev"
then
m="windev"
fi





message="${subject}: $m"

payload="payload={\"channel\": \"${to_chanel//\"/\\\"}\", \"username\": \"${username//\"/\\\"}\", \"text\": \"${message//\"/\\\"}\", \"icon_emoji\": \"${emoji}\"}"
curl -m 5 --data-urlencode "${payload}" $url -A 'zabbix-slack-alertscript / https://github.com/ericoc/zabbix-slack-alertscript'
payload="payload={\"channel\": \"${to//\"/\\\"}\", \"username\": \"${username//\"/\\\"}\", \"text\": \"${message//\"/\\\"}\", \"icon_emoji\": \"${emoji}\"}"
curl -m 5 --data-urlencode "${payload}" $url -A 'zabbix-slack-alertscript / https://github.com/ericoc/zabbix-slack-alertscript'
