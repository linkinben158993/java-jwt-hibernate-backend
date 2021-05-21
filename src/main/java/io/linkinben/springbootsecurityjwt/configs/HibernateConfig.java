package io.linkinben.springbootsecurityjwt.configs;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.linkinben.springbootsecurityjwt.StringConstants;

@Configuration
@EnableTransactionManagement()
public class HibernateConfig {

//	@Bean
//	public DataSource dataSource() {
//
//		DriverManagerDataSource dataSource = new DriverManagerDataSource();
//
//		dataSource.setDriverClassName(StringConstants.DATA_SOURCE_DRIVER_CLASS_NAME);
//
//		dataSource.setUrl(StringConstants.DATA_SOURCE_URL);
//
//		dataSource.setUsername(StringConstants.DATA_SOURCE_USER_NAME);
//
//		dataSource.setPassword(StringConstants.DATA_SOURCE_PASSWORD);
//
//		return dataSource;
//	}
	
	@Autowired
	public DataSource dataSource;

	@Bean
	public LocalSessionFactoryBean sessionFactory() {
		LocalSessionFactoryBean bean = new LocalSessionFactoryBean();

//		bean.setDataSource(dataSource());
		bean.setDataSource(dataSource);

		bean.setPackagesToScan("io.linkinben");

		Properties properties = new Properties();

		properties.put(StringConstants.PROPS_KEY_DIALECT_PLATFORM,
				StringConstants.PROPS_VALUE_DIALECT_PLATFORM_DATABASE);

		// "create" at first run and change to "update" later
		properties.put(StringConstants.PROPS_KEY_AUTOCREATE_DATABASE, "update");
		
		properties.put(StringConstants.PROPS_KEY_DRIVERCLASSNAME, StringConstants.PROPS_VALUE_DRIVERCLASSNAME);
		
		properties.put(StringConstants.PROPS_KEY_DIALECT, StringConstants.PROPS_VALUE_DIALECT_DATABASE);

		properties.put(StringConstants.PROPS_KEY_SHOW_SQL, false);

		properties.put(StringConstants.PROPS_KEY_FORMAT, true);

		properties.put(StringConstants.PROPS_KEY_JPA_OPEN_IN_VIEW, false);
		
		bean.setHibernateProperties(properties);

		return bean;
	}

	@Bean
	public HibernateTransactionManager transactionManager() {
		HibernateTransactionManager manager = new HibernateTransactionManager();
		manager.setSessionFactory(sessionFactory().getObject());
		return manager;
	}
}
