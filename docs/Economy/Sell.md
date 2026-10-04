# Sell

Players can list their regions for sale to other players. A sale is a direct payment between two players: the buyer pays the listed price from their personal balance, the seller receives that money, and ownership of the region transfers in a single step.

## How It Works

1. **Seller** places a sale sign with the asking price.
2. **Buyer** right-clicks the sign and pays the listed price.
3. **Ownership** transfers completely to the buyer; the seller receives the payment.
4. **Settings** remain with the region — members, flags, and the bank all carry over.

!!! warning "Permanent Transfer"

    When sold, the original owner loses all access. Make sure before listing!

## Via Signs

Place a sign inside the region you own:

```
Line 1: [HSell]
Line 2: (Region name)
Line 3: (Price: e.g. 15000)
Line 4: (Leave empty)
```

The price may be written plainly (`15000`) or with formatting (`$15,000`, `15k`, `2.5m`); anything else on that line is rejected with an error.

---

## Configuration

```yaml
# In regions.yml
selling:
  enabled: true
  min-sell: 10.0              # Lowest allowed sale price
  max-sell: 1000000000.0      # Highest allowed sale price
```

`enabled` is the master switch:

- **true** (default): Sale signs can be placed and purchases go through
- **false**: Sale signs are rejected when you try to place them

`min-sell` and `max-sell` bound the price a seller may ask; any sign written with a price outside this range is rejected.

## Cooldown

Ownership transfers (including purchases made through sale signs) share a cooldown so a single player cannot chain-buy regions:

```yaml
# In regions.yml
cooldown:
  region-transfer-ownership:
    ignore-operators: true    # Operators bypass the cooldown
    value: 604800             # 7 days (seconds)
```

After a purchase, the buyer must wait out the cooldown before buying another region. Operators bypass the cooldown when `ignore-operators` is `true`. Restarting the server resets all cooldowns.

