package org.elvis.wang.mybatis.page.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.elvis.wang.mybatis.page.dialect.Dialect;
import org.elvis.wang.mybatis.page.model.Page;
import org.elvis.wang.mybatis.page.parser.PageParser;
import org.elvis.wang.mybatis.page.parser.impl.MysqlPageParser;
import org.elvis.wang.mybatis.page.parser.impl.OraclePageParser;
import org.elvis.wang.mybatis.page.parser.impl.PostgresqlPageParser;
import org.elvis.wang.mybatis.page.parser.impl.SqlserverPageParser;

import java.sql.Connection;
import java.util.Properties;


/**
 * 判断是否存在Page参数，如果存在，则pageParser改写为分页的sql
 *
 */

@SuppressWarnings({"rawtypes"})
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class})})
public class PageSqlRewriteInterceptor implements Interceptor {
	
	private PageParser pageParser;
	
	private static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	
	private static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		// 获取分页参数
		Page page = PageThreadLocal.get();
		if (page != null) {
			StatementHandler statementHandler = (StatementHandler) invocation.getTarget();  
			BoundSql boundSql = statementHandler.getBoundSql();
			
			// 设置sql为分页sql
			MetaObject metaBoundSql = MetaObject.forObject(boundSql, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
			metaBoundSql.setValue("sql", pageParser.getPageSql(boundSql.getSql(), page.getFirst(), page.getPageSize()));
		}
		return invocation.proceed();
	}
	
	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
	}

	/**
	 * 优先使用mybatis配置文件中设置的property属性, 获取不到，从系统参数中获取，默认为mysql
	 * 使用spring集成可以直接设置PageParser对象
	 */
	@Override
	public void setProperties(Properties properties) {
		if (pageParser == null) {
			//数据库方言
	        String strDialect = properties.getProperty("db.dialect");
	        if (strDialect == null) {
	        	strDialect = System.getProperty("db.dialect", Dialect.mysql.name());
	        }
	        Dialect dialect = Dialect.valueOf(strDialect);
	        this.pageParser = getPageParser(dialect);
		}
	}

	public PageParser getPageParser() {
		return pageParser;
	}

	public void setPageParser(PageParser pageParser) {
		this.pageParser = pageParser;
	}
	
	public void setDialect(String dialect) {
		Dialect dbDialect = Dialect.valueOf(dialect.toLowerCase());
        this.pageParser = getPageParser(dbDialect);
	}
	
	private PageParser getPageParser(Dialect dbDialect) {
		PageParser pageParser = null;
        switch (dbDialect) {
	        case mysql:
	        	pageParser = new MysqlPageParser();
	            break;
	        case oracle:
	        	pageParser = new OraclePageParser();
	            break;
	        case postgresql:
	        	pageParser = new PostgresqlPageParser();
	            break;
	        case sqlserver:
	        	pageParser = new SqlserverPageParser();
	            break;
	        default:
	        	pageParser = new MysqlPageParser();
        }
        return pageParser;
	}
	
}
