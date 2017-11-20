package org.elvis.wang.mybatis.page.parser;

public interface PageParser {

	/**
	 * 拼接为分页sql
	 * @param sql
	 * @param offset
	 * @param limit
	 * @return
	 */
    public String getPageSql(String sql, int offset, int limit);
	
}
