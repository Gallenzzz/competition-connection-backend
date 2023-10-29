package com.gallen.common.constants;

public class ArticleConstants {
    public static final Integer ARTICLE_STATUS_UNCHECKED = 0;
    public static final Integer ARTICLE_STATUS_UNPASS = 1;
    public static final Integer ARTICLE_STATUS_PASS = 2;

    public static final String ARTICLE_HOT_ARTICLE_SCHEDULE_LOCK = "cc:article:hot:article:schedule:lock";
    public static final String ARTICLE_SCHEDULE_PUBLISH_LOCK = "cc:article:schedule:publish:lock";

    public static final Integer ARTICLE_CHECK_AFTER_MILLISECONDS = 10 * 60 * 1000;
}
