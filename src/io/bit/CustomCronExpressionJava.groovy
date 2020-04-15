package io.bit

@Grab('org.quartz-scheduler:quartz:2.1.6')
import org.quartz.*

public class CustomCronExpressionJava
{
    Date getNextValidTimeAfter(String cron, Date date) {
        CronExpression cronExpression = new CronExpression(cron)
        return cronExpression.getNextValidTimeAfter(date)
    }
}