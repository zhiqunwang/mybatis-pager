package org.elvis.wang.mybatis.page.parser.impl;


import org.elvis.wang.mybatis.page.parser.PageParser;

public class SqlserverPageParser implements PageParser {

	@Override
	public String getPageSql(String sql, int offset, int limit) {
		if (offset <= 0 && limit <= 0) {
			return sql;
		}
		
		// 查找sql语句的第一个select
		sql = sql.trim();
		String caseSensitiveSql = sql.trim().toUpperCase();
		int selectPosition = caseSensitiveSql.indexOf("SELECT");
		if (selectPosition < 0) {
			return sql;
		}
		
		// 拼接sqlserver的分页查询语句
		StringBuilder sb = new StringBuilder(sql.length() + 80);
		if (offset > 0) {
			sb.append("SELECT * FROM ( ")
			  .append("SELECT ROW_NUMBER()OVER(ORDER BY __tc__)__rn__,* FROM ( ")
			  .append("SELECT TOP ").append(offset + limit).append(" 0 __tc__, ").append(sql.substring(6))
			  .append(") t").append(" )tt ")
			  .append(" WHERE __rn__ > ").append(offset);
		} else {
			sb.append("SELECT TOP ").append(limit).append(" ")
			  .append(sql.substring(6));
		}
		return sb.toString();
	}
}
