"""Validate actual I2 integration responses against the OS baseline, offline.

Usage: python scripts/validate_i2_contracts.py /path/to/numberone-service-order doc/evidence/i2/integration.json
Requires the dependencies from OS scripts/requirements-contracts.lock.
"""
import json
import socket
import sys
from pathlib import Path

os_checkout = Path(sys.argv[1]).resolve()
sys.path.insert(0, str(os_checkout / "scripts"))
from contract_revision_checks import SPECS, inline_openapi, CHECKER
from openapi_schema_validator import OAS30Validator


def deny_network(*args, **kwargs):
    raise RuntimeError("Network is disabled")


socket.getaddrinfo = deny_network
evidence = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8-sig"))
spec = SPECS["inventory.openapi.yaml"]
assert spec["info"]["version"] == "2.0.0-os-baseline.1"
checked = 0
for name in ("closed", "reversed", "rejected"):
    schema = inline_openapi(spec, spec["components"]["schemas"]["InventoryOperation"])
    OAS30Validator(schema, format_checker=CHECKER).validate(evidence[name])
    checked += 1
for operation in evidence["osStatus"]["operations"]:
    if operation["intent"] == "RESOLVE_STOCK":
        OAS30Validator(schema, format_checker=CHECKER).validate(operation["result"])
        checked += 1
assert checked == 6
print(json.dumps({"contract": spec["info"]["version"], "realResponses": checked,
                  "valid": True, "participants": "real OS + real Inventory + simulated Billing"}, indent=2))
