create table if not exists orders (
    id identity primary key,
    symbol varchar(20),
    side varchar(20),
    type varchar(20),
    price numeric(12, 6),
    quantity numeric(12, 8),
    origin_order_id varchar(36),
    exchange_order_id varchar(36),
    time_frame varchar(10),
    stop_loss numeric(12, 6),
    take_profit numeric(12, 6),
    state varchar(36),
    last_update timestamp default now(),
    account_code varchar(256)
);

alter table orders add constraint if not exists orders_uq unique (origin_order_id);

create table if not exists exchange_symbol_info (
    symbol varchar(20) not null,
    exchange varchar(256) not null,
    pair varchar(20),
    tick_size numeric(12, 6),
    step_size numeric(12, 8),
    last_update timestamp default now(),
    version integer,
    primary key(symbol)
);

alter table exchange_symbol_info add constraint if not exists exchange_symbol_info_uq unique (symbol, exchange);

create table if not exists accounts (
    id identity primary key,
    code varchar(256) not null,
    exchange varchar(256) not null,
    api_key varchar not null,
    secret_key varchar not null,
    active boolean not null default true,
    master boolean not null default false,
    last_update timestamp default now()
    );
alter table accounts add constraint if not exists exchange_symbol_info_uq unique (code, exchange);


alter table if exists orders alter column if exists ex_bridge_account rename to account_code;
alter table if exists orders alter column if exists account_code set not null;