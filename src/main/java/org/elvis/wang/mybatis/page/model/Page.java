package org.elvis.wang.mybatis.page.model;

import java.util.ArrayList;
import java.util.List;

public class Page<T> implements java.io.Serializable{

	private static final long serialVersionUID = 1642092599064800597L;
	
	public static final int DEFUALT_PAGESIZE = 10;
	public static final int MIN_PAGESIZE = 1;
	public static final int MAX_PAGESIZE = 5000;

	// 分页参数
	protected int pageNo = 1;
	protected int pageSize = DEFUALT_PAGESIZE;

	protected List<T> result = null;
	protected int totalCount = 0;

	// 构造函数

	public Page() {
		super();
	}

	public Page(int pageSize) {
		setPageSize(pageSize);
	}
	
	public Page(int pageSize, int pageNo) {
		setPageSize(pageSize);
		setPageNo(pageNo);
	}

	/**
	 * 获得当前页的页号,序号从1开始,默认为1.
	 */
	public int getPageNo() {
		return pageNo;
	}

	/**
	 * 设置当前页的页号,序号从1开始,低于1时自动调整为1.
	 */
	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;

		if (pageNo < 1) {
			this.pageNo = 1;
		}
	}

	/**
	 * 获得每页的记录数量,默认为20.
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * 设置每页的记录数量,超出MIN_PAGESIZE与MAX_PAGESIZE范围时会自动调整.
	 */
	public void setPageSize(final int pageSize) {
		if (pageSize < MIN_PAGESIZE) {
			this.pageSize = MIN_PAGESIZE;
		} else if (pageSize > MAX_PAGESIZE) {
			this.pageSize = MAX_PAGESIZE;
		} else if (0 == pageSize) {
			this.pageSize = DEFUALT_PAGESIZE;
		} else {
			this.pageSize = pageSize;
		}
	}

	/**
	 * 根据pageNo和pageSize计算当前页第一条记录在总结果集中的位置,序号从0开始.
	 */
	public int getFirst() {
		return ((pageNo - 1) * pageSize);
	}

	/**
	 * 根据pageSize与totalCount计算总页数,默认值为-1.
	 */
	
	public int getTotalPages() {
		if (totalCount <= 0)
			return 1;

		int count = totalCount / pageSize;
		if (totalCount % pageSize > 0) {
			count++;
		}
		return count;
	}

	/**
	 * 是否还有下一页.
	 */
	
	public boolean isHasNext() {
		return (pageNo + 1 <= getTotalPages());
	}

	/**
	 * 取得下页的页号,序号从1开始.
	 */
	
	public int getNextPage() {
		if (isHasNext())
			return pageNo + 1;
		else
			return pageNo;
	}

	/**
	 * 是否还有上一页.
	 */
	
	public boolean isHasPre() {
		return (pageNo - 1 >= 1);
	}

	/**
	 * 取得上页的页号,序号从1开始.
	 */
	
	public int getPrePage() {
		if (isHasPre())
			return pageNo - 1;
		else
			return pageNo;
	}

	/**
	 * 取得页内的记录列表.
	 */
	public List<T> getResult() {
		if (result == null)
			result = new ArrayList<T>();
		return result;
	}

	public void setResult(final List<T> result) {
		this.result = result;
	}

	/**
	 * 取得总记录数,默认值为-1.
	 */
	public int getTotalCount() {
		if (totalCount < 0)
			totalCount = 0;
		return totalCount;
	}

	public void setTotalCount(final int totalCount) {
		this.totalCount = totalCount;
	}
}
