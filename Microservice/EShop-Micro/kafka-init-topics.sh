#!/usr/bin/env bash
set -euo pipefail

BROKER=eshop-kafka:9092

create() {
  local topic="$1"
  local partitions="${2:-3}"
  docker exec -it eshop-kafka /opt/bitnami/kafka/bin/kafka-topics.sh --create --if-not-exists     --bootstrap-server "$BROKER" --topic "$topic" --partitions "$partitions" --replication-factor 1
}

# CART
create eshop.cart.get.req 6
create eshop.cart.get.resp 6
create eshop.cart.clear.cmd 6

# SHIPPING
create eshop.shipping.rate.req 3
create eshop.shipping.rate.resp 3

# CATALOG
create eshop.catalog.product.snapshot.req 6
create eshop.catalog.product.snapshot.resp 6

# CUSTOMER ADDRESS
create eshop.customer.address.req 3
create eshop.customer.address.resp 3

# SETTINGS
create eshop.settings.email.req 1
create eshop.settings.email.resp 1
create eshop.settings.paypal.req 1
create eshop.settings.paypal.resp 1

# ORDER EVENTS
create eshop.order.events 6
create eshop.order.paid.events 6

# ORDER QUERY (review)
create eshop.order.hasPurchased.req 3
create eshop.order.hasPurchased.resp 3
