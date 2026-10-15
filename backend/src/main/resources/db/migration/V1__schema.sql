create table app_user (
    id            bigint generated always as identity primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(100) not null,
    full_name     varchar(100) not null,
    role          varchar(20)  not null check (role in ('EMPLOYEE', 'FRONT_DESK', 'ADMIN')),
    email         varchar(100) not null,
    phone         varchar(30)  not null
);

create table visit (
    id             bigint generated always as identity primary key,
    type           varchar(20)  not null check (type in ('PRE_REGISTERED', 'WALK_IN')),
    state          varchar(20)  not null check (state in ('EXPECTED', 'AWAITING_HOST', 'APPROVED', 'DECLINED',
                                                          'TIMED_OUT', 'CHECKED_IN', 'CHECKED_OUT')),
    visitor_name   varchar(100) not null,
    visitor_phone  varchar(30),
    purpose        varchar(200) not null,
    host_id        bigint       not null references app_user (id),
    visit_date     date         not null,
    pass_number    varchar(20),
    created_at     timestamptz  not null,
    arrived_at     timestamptz,
    checked_in_at  timestamptz,
    checked_out_at timestamptz,
    version        bigint       not null,
    check (state not in ('CHECKED_IN', 'CHECKED_OUT') or (pass_number is not null and checked_in_at is not null)),
    check (state <> 'CHECKED_OUT' or checked_out_at is not null)
);

-- A lanyard card is held by at most one visitor on campus; it is free again after check-out.
create unique index visit_active_pass_uq on visit (pass_number) where state = 'CHECKED_IN';

create table notification (
    id           bigint generated always as identity primary key,
    recipient_id bigint       not null references app_user (id),
    message      varchar(500) not null,
    created_at   timestamptz  not null
);

create table audit_event (
    id          bigint generated always as identity primary key,
    occurred_at timestamptz  not null,
    actor       varchar(50)  not null,
    action      varchar(40)  not null,
    visit_id    bigint references visit (id),
    details     varchar(500)
);
