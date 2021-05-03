package io.linkinben.springbootsecurityjwt;

public class StringConstants {
	// Hibernate Config
	public static String DATA_SOURCE_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
	// Local usage
	public static String DATA_SOURCE_URL = "jdbc:mysql://localhost:3306/jwt_db?createDatabaseIfNotExist=true&useSSL=true";
	public static String DATA_SOURCE_USER_NAME = "root";
	public static String DATA_SOURCE_PASSWORD = "Thienanvip@321";

	// Production
//	public static String DATA_SOURCE_URL = "jdbc:mysql://sql6.freemysqlhosting.net:3306/sql6397487?createDatabaseIfNotExist=true&useSSL=true";
//	public static String DATA_SOURCE_USER_NAME = "sql6397487";
//	public static String DATA_SOURCE_PASSWORD = "pIaQup6sIc";

	// Session Factory
	// Chỉ ra gói để quét
	public static String SESSION_FACTORY_PACKAGE_TO_SCAN = "io.linkinben.springbootsecurityjwt.entities";
	public static String PROPS_KEY_DIALECT_PLATFORM = "spring.jpa.database-platform";
	public static String PROPS_KEY_DIALECT = "spring.jpa.properties.hibernate.dialect";
	public static String PROPS_KEY_SHOW_SQL = "hibernate.show_sql";
	public static String PROPS_KEY_FORMAT = "hibernate.formate_sql";
	// spring.jpa.hibernate.ddl-auto Do more research
	public static String PROPS_KEY_AUTOCREATE_DATABASE = "hibernate.hbm2ddl.auto";
	public static String PROPS_KEY_DRIVERCLASSNAME = "spring.datasource.driverClassName";
	// Dialect của MySQL
	public static String PROPS_DIALECT_PLATFORM_DATABASE = "spring.jpa.database-platform";
	public static String PROPS_VALUE_DIALECT_PLATFORM_DATABASE = "org.hibernate.dialect.MySQLDBDialect";
	public static String PROPS_VALUE_DIALECT_DATABASE = "org.hibernate.dialect.MySQL55Dialect";
	public static String PROPS_VALUE_DRIVERCLASSNAME = "com.mysql.jdbc.Driver";
	// Notice
	public static String PROPS_KEY_JPA_OPEN_IN_VIEW = "spring.jpa.open-in-view";

	// Character Encoding
	public static String CHARACTER_CODING = "UTF-8";

	// Myclass Config
	public static String TEMPLATE_RESOLVER_PREFIX = "/WEB-INF/templates/";
	public static String TEMPLATE_RESOLVER_SUFFIX = ".html";
	public static String TEMPLATE_RESOLVER_TEMPLATE_MODE = "HTML5";
	public static String TEMPLATE_RESOLVER_RESOURCE_LOCATION = "/WEB-INF/assets/";

}
