package org.elvis.wang.mybatis.page.interceptor;


import org.elvis.wang.mybatis.page.model.Page;

@SuppressWarnings("rawtypes")
public class PageThreadLocal {

	private static final ThreadLocal<Page> PAGE_THREAD_LOCAL = new ThreadLocal<Page>();
	
	public static void set(Page page) {
		PAGE_THREAD_LOCAL.set(page);
	}
	
	public static Page get() {
		return PAGE_THREAD_LOCAL.get();
	}
	
	public static void clear() {
		PAGE_THREAD_LOCAL.remove();
	}
}
