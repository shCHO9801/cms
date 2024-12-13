package com.zerobase.cms.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@ServletComponentScan
@EnableJpaRepositories(repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
@EnableJpaAuditing
@SpringBootApplication
@EnableFeignClients
public class ZeroOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZeroOrderApplication.class, args);
    }
}
