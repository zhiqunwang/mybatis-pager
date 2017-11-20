package org.elvis.wang.mybatis.page.interceptor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.elvis.wang.mybatis.page.model.Page;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 拦截Executor的query方法, 从参数中获取Page参数, 存在page参数，
 * 则进行分页查询, 不存在, 按照正常的查询执行
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Intercepts({@Signature(type = Executor.class, method = "query", 
					   args = {MappedStatement.class, 
	                           Object.class, 
	                           RowBounds.class, 
	                           ResultHandler.class}),
	         @Signature(type = Executor.class, method = "update", 
	         			args = {MappedStatement.class, Object.class })
	        }
	        )

public class PageInterceptor implements Interceptor {
	
	// 查询总数的statment的id后缀
    private static final String SUFFIX_COUNT = "_COUNT";
	
	private static final ConcurrentMap<String, MappedStatement> PAGE_COUNT_MAPPEDSTATEMENT = new ConcurrentHashMap<String, MappedStatement>();
	
	private int notPagedQueryMaxSize = 1000;
	
	private boolean checkUpdateWhereClause = false;
	
	private boolean checkNotPagedQuery = false;
	
	public void setCheckNotPagedQuery(boolean checkNotPagedQuery) {
		this.checkNotPagedQuery = checkNotPagedQuery;
	}

	public void setNotPagedQueryMaxSize(int notPagedQueryMaxSize) {
		if (notPagedQueryMaxSize < 1000) {
			this.notPagedQueryMaxSize = notPagedQueryMaxSize;
		}
	}
	
	public void setCheckUpdateWhereClause(boolean checkWhereClause) {
		this.checkUpdateWhereClause = checkWhereClause;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		//获取MappedStatement和查询参数
	    MappedStatement mappedStatement= (MappedStatement)invocation.getArgs()[0];  
	    
	    // 判断执行sql种类
	    SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
	    if (sqlCommandType == SqlCommandType.SELECT) {
	    	return doSelectProcess(invocation);
	    } else if (sqlCommandType == SqlCommandType.UPDATE || sqlCommandType == SqlCommandType.DELETE) {
	    	return doUpdateProcess(invocation);
	    }
	    return invocation.proceed();
	}
	
	private Object doUpdateProcess(Invocation invocation) throws Throwable {
		// 解析批量更新sql会有问题, 此功能暂时先关闭
		if (checkUpdateWhereClause) {
			MappedStatement mappedStatement = (MappedStatement)invocation.getArgs()[0];  
			BoundSql boundSql = mappedStatement.getBoundSql(invocation.getArgs()[1]);
			
			// 解析sql，获取where子句， 不存在where子句, 则抛出异常
			Statement stmt = CCJSqlParserUtil.parse(boundSql.getSql());
			
			Expression where = null;
			if (stmt instanceof Update) {
				where = ((Update) stmt).getWhere();
			} else if (stmt instanceof Delete) {
				where = ((Delete) stmt).getWhere();
			}
			
			if (where == null) {
				throw new ExecutorException(MessageFormat.format("the statment id [{0}] not constains where clause", mappedStatement.getId()));
			}
		}
		return invocation.proceed();
	}

	private Object doSelectProcess(Invocation invocation) throws Throwable {
		MappedStatement mappedStatement= (MappedStatement)invocation.getArgs()[0];
		Object parameter = invocation.getArgs()[1];   

	    // 获取分页参数
 	    Page page = getPageParamter(parameter);
	    
        // 非分页查询, 直接执行
        if (page == null) {
        	// 防止查询不带条件, 此处强制设置分页, 最大查询1000条
        	if (checkNotPagedQuery) {
        		try {
            		page = new Page();
            		page.setPageNo(1);
            		page.setPageSize(notPagedQueryMaxSize);
                	PageThreadLocal.set(page);
                	return invocation.proceed();
            	} catch (Throwable e) {
            		throw e;
            	} finally {
            		PageThreadLocal.clear();
            	}
        	} else {
        		return invocation.proceed();
        	}
        }
    	
    	// 判断参数，如果只有一个真实的参数，则直接用这个参数
    	if (parameter instanceof Map) {
    		// 判断xml配置中parameterMap的type不为map, 并且只有一个参数，则直接用真实的参数
    		Class paramterType = mappedStatement.getParameterMap().getType();
    		if (paramterType != null && !Map.class.isAssignableFrom(paramterType)) {
    			Map<String, Object> paramMap = (Map<String, Object>)parameter;
        		Set<Object> paramValueSet = new HashSet<Object>(paramMap.values());
    			if (paramValueSet.size() == 1) {
    				invocation.getArgs()[1] = paramValueSet.iterator().next();
    			}
    		} else {
				Map<String, Object> paramMap = (Map<String, Object>)parameter;
        		Set<Object> paramValueSet = new HashSet<Object>(paramMap.values());
    			if (paramValueSet.size() == 1) {
    				if (paramValueSet.iterator().next() instanceof Map) {
    					invocation.getArgs()[1] = paramValueSet.iterator().next();
    				}
    			}
    		}
    	} else {
    		// 如果只有page参数，则剔除page参数
    		if (parameter == page) {
    			invocation.getArgs()[1] = null;
    		}
    	}
    	
    	// 获取分页获取总数
    	MappedStatement pageCountMappedStatement = getPageCountMappedStatement(mappedStatement);
    	invocation.getArgs()[0] = pageCountMappedStatement;
    	
        // 查询总数
        Object result = invocation.proceed();
        
        // 设置总数， 总数为0, 直接返回
        page.setTotalCount((Integer) ((List) result).get(0));
        if (page.getTotalCount() == 0) {
        	page.setResult(new ArrayList());
        } else {
        	try {
        		invocation.getArgs()[0] = mappedStatement;
            	PageThreadLocal.set(page);
            	// 获取分页数据
    			result = invocation.proceed();
    			page.setResult((List) result);
        	} catch (Throwable e) {
        		throw e;
        	} finally {
        		PageThreadLocal.clear();
        	}
        }
        return page.getResult();
	}

	/**
	 * 获取分页查询总数的mappedStatement
	 * @param mappedStatement
	 * @return
	 */
	private MappedStatement getPageCountMappedStatement(MappedStatement mappedStatement) {
		String pageCountSelectKey = mappedStatement.getId() + SUFFIX_COUNT;
		MappedStatement pageCountMappedStatement = PAGE_COUNT_MAPPEDSTATEMENT.get(pageCountSelectKey);
		if (pageCountMappedStatement == null) {
			PAGE_COUNT_MAPPEDSTATEMENT.putIfAbsent(pageCountSelectKey, mappedStatement.getConfiguration().getMappedStatement(pageCountSelectKey));
			pageCountMappedStatement = PAGE_COUNT_MAPPEDSTATEMENT.get(pageCountSelectKey);
		}
		
		if (pageCountMappedStatement == null) {
    		throw ExceptionFactory.wrapException("No statment id " + pageCountSelectKey, new NullPointerException());
    	}
		return pageCountMappedStatement;
	}

	/**
	 * 获取Page分页参数
	 * @param parameter
	 * @return
	 */
	private Page getPageParamter(Object parameter) {
		Page page = null;
		if (parameter == null) {
			return null;
		} 
		
		// 查询参数为map, 从map中获取value为Page的对象
		if (parameter instanceof Map) {
			Map<String, Object> paramMap = (Map)parameter;
			for (Iterator<Map.Entry<String, Object>> iterator = paramMap.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<String, Object> entry = iterator.next();
				if (entry.getValue() instanceof Page) {
					page = (Page) entry.getValue();
					iterator.remove();
				}
			}
			return page;
		} else if (parameter instanceof Page) {
			return (Page)parameter;
		} else {
			// TODO parameter可能为java bean, 从java bean中获取Page参数
			// 返回获取parameter中的getPage的方法
			try {
				Method getPageMethod = parameter.getClass().getMethod("getPage");
				if (getPageMethod.getReturnType() == Page.class) {
					page = (Page) getPageMethod.invoke(parameter);
				}
			} catch (Exception e) {
				// 获取不到page参数，直接不处理
			}
			return page;
		}
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
	}

	@Override
	public void setProperties(Properties properties) {
		String notPagedQueryMaxSizeConfig = properties.getProperty("notPagedQueryMaxSize");
		int notPagedQueryMaxSizeParam = 0;
        if (notPagedQueryMaxSizeConfig != null && !notPagedQueryMaxSizeConfig.trim().isEmpty()) {
        	try {
        		notPagedQueryMaxSizeParam = Integer.parseInt(notPagedQueryMaxSizeConfig);
        		setNotPagedQueryMaxSize(notPagedQueryMaxSizeParam);
        	} catch (Exception e) {
        		// ignore
        	}
        }
        
        String checkUpdateWhereClauseConfig = properties.getProperty("checkUpdateWhereClause");
        boolean checkUpdateWhereClauseParam = false;
        if (notPagedQueryMaxSizeConfig != null && !notPagedQueryMaxSizeConfig.trim().isEmpty()) {
		    try {
		    	checkUpdateWhereClauseParam = Boolean.parseBoolean(checkUpdateWhereClauseConfig);
		    	setCheckUpdateWhereClause(checkUpdateWhereClauseParam);
		    } catch (Exception e) {
				// ignore
			}
        }
        
        String checkNotPagedQueryConfig = properties.getProperty("checkNotPagedQuery");
        boolean checkNotPagedQueryParam = false;
        if (checkNotPagedQueryConfig != null && !checkNotPagedQueryConfig.trim().isEmpty()) {
		    try {
		    	checkNotPagedQueryParam = Boolean.parseBoolean(checkNotPagedQueryConfig);
		    	setCheckNotPagedQuery(checkNotPagedQueryParam);
		    } catch (Exception e) {
				// ignore
			}
        }
	}
}
