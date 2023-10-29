create table tb_user
(
    role        tinyint       default 1                 not null comment '0：管理员，1：普通用户',
    create_time datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime      default CURRENT_TIMESTAMP not null comment '更新时间',
    create_user bigint                                  null comment '创建者',
    update_user bigint                                  null comment '更新者',
    is_deleted  tinyint       default 0                 not null comment '逻辑删除，0：未删除，1：已删除',
    email       varchar(64)                             null,
    id          bigint                                  not null comment '主键',
    username    varchar(16)                             not null comment '用户名唯一',
    password    varchar(32)                             not null comment 'MD5加密密码',
    tag         varchar(1024) default ''                not null comment '标签JSON列表字符串',
    gender      tinyint       default 2                 not null comment '性别',
    avatar      varchar(512)  default ''                null comment '头像',
    phone       varchar(11)   default ''                not null comment '手机号码',
    salt        varchar(32)                             not null comment '密码加密盐',
    nickname    varchar(32)                             null comment '昵称',
    status      tinyint       default 0                 not null comment '0：正常，1：禁用',
    constraint tb_user_id_uindex
        unique (id),
    constraint tb_user_username_uindex
        unique (username)
)
    comment '用户信息表';

alter table tb_user
    add primary key (id);

-- auto-generated definition
create table tb_user_team
(
    is_delete   tinyint  default 0                 null comment '是否删除',
    update_time datetime default CURRENT_TIMESTAMP null comment '更新时间',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    join_time   datetime                           null comment '加入队伍时间',
    team_id     bigint                             not null comment '队伍id',
    user_id     bigint                             not null comment '用户id',
    id          bigint auto_increment
        primary key
)
    comment '用户-队伍关系表';

-- auto-generated definition
create table tb_team
(
    user_id     bigint                             null comment '创建者id',
    is_delete   tinyint  default 0                 null comment '0 - 未删除， 1 - 已删除',
    update_time datetime default CURRENT_TIMESTAMP null comment '更新时间',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    password    varchar(512)                       null comment '密码',
    status      int      default 0                 not null comment '0 - 公开， 1 - 私有， 2 - 加密',
    expire_time datetime                           null comment '队伍过期时间',
    max_num     int      default 1                 null comment '队伍最大人数',
    description varchar(1024)                      null comment '队伍描述',
    name        varchar(256)                       not null comment '队伍名称',
    id          bigint auto_increment
        primary key
)
    comment '队伍信息表';

-- auto-generated definition
create table tb_user_tag
(
    id      bigint auto_increment,
    user_id bigint null comment '用户id',
    tag_id  bigint null comment '标签id',
    constraint tb_user_tag_id_uindex
        unique (id)
);

alter table tb_user_tag
    add primary key (id);

-- auto-generated definition
create table tb_tag
(
    create_user bigint                             null comment '创建者id',
    update_user bigint                             null comment '更新者id',
    is_deleted  tinyint  default 0                 null comment '是否逻辑删除，0:false，1:true',
    update_time datetime default CURRENT_TIMESTAMP null comment '更新日期时间',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建日期时间',
    parent_id   bigint   default 0                 null comment '父标签id',
    tag_name    varchar(16)                        not null comment '标签名称',
    id          bigint auto_increment comment '唯一标识',
    constraint tb_tag_id_uindex
        unique (id)
)
    comment '标签表';

alter table tb_tag
    add primary key (id);

-- auto-generated definition
create table tb_article
(
    publish_time datetime          null comment '定时发布时间',
    channel_id   bigint            not null comment '频道id',
    is_delete    tinyint default 0 not null comment '是否删除，0:未删除，1:删除',
    update_user  bigint            null comment '更新者id',
    create_user  bigint            null comment '创建者id',
    update_time  datetime          null comment '更新日期时间',
    create_time  datetime          null comment '创建日期时间',
    status       tinyint default 0 not null comment '文章状态，0:正常，1:下架',
    collections  int     default 0 not null comment '收藏数量',
    likes        int     default 0 not null comment '点赞数量',
    comments     int     default 0 not null comment '评论数量',
    views        bigint  default 0 not null comment '阅读量',
    content      longtext          not null comment '文章html内容',
    cover        varchar(512)      null comment '封面',
    title        varchar(64)       not null comment '文章标题',
    id           bigint auto_increment,
    constraint tb_article_id_uindex
        unique (id)
)
    comment '文章表';

alter table tb_article
    add primary key (id);

-- auto-generated definition
create table tb_article_check
(
    id           bigint auto_increment,
    article_id   bigint            not null comment '文章id',
    text         longtext          null comment '审核的文本内容',
    publish_time datetime          null comment '发布时间',
    status       tinyint default 0 null comment '审核状态，0：未审核，1：审核不通过，2：审核通过',
    constraint tb_task_info_id_uindex
        unique (id)
)
    comment '文章审核表';

alter table tb_article_check
    add primary key (id);

-- auto-generated definition
create table tb_article_content
(
    id         bigint auto_increment,
    content    longtext null comment '文章详情文本内容',
    article_id bigint   null comment '文章id',
    constraint tb_article_content_id_uindex
        unique (id)
)
    comment '文章详情表';

alter table tb_article_content
    add primary key (id);

-- auto-generated definition
create table tb_article_like
(
    id         bigint auto_increment
        primary key,
    article_id bigint   null comment '文章id',
    user_id    bigint   null comment '用户id',
    like_time  datetime null comment '点赞时间'
)
    comment '用户点赞文章关系表';

-- auto-generated definition
create table tb_channel
(
    is_delete   tinyint default 0 not null comment '0:未删除，1:已删除',
    update_user bigint            not null comment '更新者id',
    create_user bigint            not null comment '创建者id',
    update_time datetime          not null comment '更新日期时间',
    create_time datetime          not null comment '创建日期时间',
    name        varchar(16)       not null comment '频道名称',
    id          bigint auto_increment,
    constraint tb_channel_id_uindex
        unique (id),
    constraint tb_channel_name_uindex
        unique (name)
)
    comment '文章频道表';

alter table tb_channel
    add primary key (id);

-- auto-generated definition
create table tb_message
(
    id        bigint                             not null,
    message   varchar(1024)                      null comment '消息文本',
    state     tinyint  default 0                 null comment '状态(0:未读，1:已读)',
    send_date datetime default CURRENT_TIMESTAMP null comment '发送时间',
    read_time timestamp                          null comment '读消息时间',
    to_id     bigint                             null comment '接受者id',
    from_id   bigint                             null comment '发送者id',
    constraint tb_message_id_uindex
        unique (id)
)
    comment '聊天消息表';

alter table tb_message
    add primary key (id);

-- auto-generated definition
create table tb_sensitive_word
(
    id            bigint auto_increment,
    word          varchar(16)                        not null comment '敏感词',
    create_timeda datetime default CURRENT_TIMESTAMP null,
    constraint tb_sensitive_word_id_uindex
        unique (id),
    constraint tb_sensitive_word_word_uindex
        unique (word)
)
    comment '敏感词表';

alter table tb_sensitive_word
    add primary key (id);


