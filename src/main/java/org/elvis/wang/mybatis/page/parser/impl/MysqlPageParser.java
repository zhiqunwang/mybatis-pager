package org.elvis.wang.mybatis.page.parser.impl;

import org.elvis.wang.mybatis.page.parser.PageParser;

public class MysqlPageParser implements PageParser {

	@Override
	public String getPageSql(String sql, int offset, int limit) {
		if (offset <= 0 && limit <= 0) {
			return sql;
		}
		StringBuilder sb = new StringBuilder(sql.length() + 80);
		
		sb.append(sql);
		if (offset > 0) {
			sb.append(" limit ").append(offset).append(",").append(limit);
		} else {
			sb.append(" limit 0, ").append(limit);
		}
		return sb.toString();
	}

}
