
## Payment Protocol

1. Fund the channel `fundChannel` 2-of-2 multisig with server

Server must sign the refund TX
Client publishes the funding TX
Client sends the funding TX to Server
10000 gwei

Off-Chain

{type: BALANCE|CLOSING, server: 0, client: 10000, i: 0, }

1
S:      0
C:  10000

2
S:   1000
C:   9000

3
S:   3000
C:   7000

When S or C closes the channel the self part becomes locked for n blocks
S or C have now time to proof they know a signed later stage commitment and claim the payment.

HTLC
H(S)
valid_until: block#

Server liefert Datei an client

Client offered diese Channel msg1 an server:
n: 4
S:      0
C:   9000
H:  H1(s) { spendableByServer when S is revealed, spendable by C when block > delay }

server signiert und liefert msg1 an client.
client signiert und liefert msg1 an server.
server antwortet mit S an client

Scenario 1:

Client published n3, Server published n4 und punished client.

Scenario 2:

H1(s) delay läuft aus und client weigert sich auf die channel balance zu commiten.
Server published n:4 mit H1
