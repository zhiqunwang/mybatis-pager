package org.elvis.wang.mybatis.page.parser.impl;


import org.elvis.wang.mybatis.page.parser.PageParser;

public class OraclePageParser implements PageParser {

	@Override
	public String getPageSql(String sql, int offset, int limit) {
		if (offset <= 0 && limit <= 0) {
			return sql;
		}
		
		
		StringBuilder sb = new StringBuilder(sql.length() + 80);
		sb.append("   SELECT * FROM ( SELECT row_.*, ROWNUM rownum_ FROM ( ");
		sb.append(sql);
		
		if (offset > 0) {
			sb.append(" ) row_ WHERE ROWNUM <= ").append(offset + limit).append(") WHERE rownum_ > ").append(offset);
		} else {
			sb.append(" ) row_ WHERE ROWNUM <= ").append(limit).append(")");
		}

		return sb.toString();
	}

}
