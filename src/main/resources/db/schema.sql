create table if not exists orders (
    id IDENTITY primary key,
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
    ex_bridge_account varchar(256)
);

alter table orders add constraint if not exists orders_uq unique (origin_order_id);
