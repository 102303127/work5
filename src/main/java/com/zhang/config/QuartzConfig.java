package com.zhang.config;

import com.zhang.component.RedisToMysqlTask;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz定时任务配置
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail RedisToMysqlQuartz() {
        // 执行定时任务
        return JobBuilder.newJob(RedisToMysqlTask.class).withIdentity("CallPayQuartzTask").storeDurably().build();
    }

    /*
    0 0 12 * * ?	每天12点触发
    0 15 10 ? * *	每天10点15分触发
    0 0/2 * * * ?	每两分钟触发一次*/
    @Bean
    public Trigger CallPayQuartzTaskTrigger() {
        return TriggerBuilder.newTrigger().forJob(RedisToMysqlQuartz())
                .withIdentity("CallPayQuartzTask")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * * * ?"))
                .build();
    }

}
