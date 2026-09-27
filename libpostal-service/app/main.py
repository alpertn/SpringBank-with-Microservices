from collections import defaultdict
import re

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from postal.parser import parse_address

LIBPOSTAL_COMMIT = "25099c506612b34b23b1bfe286ca6321fcf06f35"
app = FastAPI(title="SpringBank Address Parser", version="1.0.0")


class ParseRequest(BaseModel):
    address: str = Field(min_length=3, max_length=1000)
    countryCode: str | None = Field(default=None, min_length=2, max_length=3)


class Component(BaseModel):
    label: str
    value: str


class ParseResponse(BaseModel):
    country: str | None = None
    countryCode: str | None = None
    province: str | None = None
    district: str | None = None
    neighborhood: str | None = None
    road: str | None = None
    building: str | None = None
    buildingNumber: str | None = None
    entrance: str | None = None
    floor: str | None = None
    unit: str | None = None
    postalCode: str | None = None
    parser: str = "libpostal"
    parserVersion: str = LIBPOSTAL_COMMIT
    components: list[Component]


def first(parts: dict[str, list[str]], *labels: str) -> str | None:
    for label in labels:
        if parts.get(label):
            return ", ".join(parts[label])
    return None


def turkish_token(address: str, token: str) -> str | None:
    match = re.search(rf"(?iu)\b{token}\s*[:.]?\s*([0-9a-zçğıöşü/-]+)", address)
    return match.group(1) if match else None


def turkish_neighborhood(address: str) -> str | None:
    match = re.search(r"(?iu)\b([a-zçğıöşü]+(?:\s+[a-zçğıöşü]+){0,2})\s+(?:mahallesi|mah\.?)(?=\s|,|$)", address)
    return match.group(1) if match else None


def without_neighborhood(road: str | None, neighborhood: str | None) -> str | None:
    if not road or not neighborhood:
        return road
    return re.sub(rf"(?iu)^\s*{re.escape(neighborhood)}\s+(?:mahallesi|mah\.?)\s*", "", road).strip()


@app.get("/health/live")
def live() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/health/ready")
def ready() -> dict[str, str]:
    return {"status": "UP", "parser": "libpostal", "version": LIBPOSTAL_COMMIT}


@app.post("/v1/addresses/parse", response_model=ParseResponse)
def parse(request: ParseRequest) -> ParseResponse:
    components = [Component(value=value, label=label) for value, label in parse_address(request.address)]
    if not components:
        raise HTTPException(status_code=422, detail="Address could not be parsed")

    parts: dict[str, list[str]] = defaultdict(list)
    for component in components:
        parts[component.label].append(component.value)

    neighborhood = first(parts, "suburb") or turkish_neighborhood(request.address)
    road = without_neighborhood(first(parts, "road"), neighborhood)

    return ParseResponse(
        country=first(parts, "country"),
        countryCode=request.countryCode.upper() if request.countryCode else None,
        province=first(parts, "state", "city"),
        district=first(parts, "city_district", "state_district"),
        neighborhood=neighborhood,
        road=road,
        building=first(parts, "house"),
        buildingNumber=turkish_token(request.address, "(?:no|numara)") or first(parts, "house_number"),
        entrance=first(parts, "entrance", "staircase"),
        floor=turkish_token(request.address, "kat") or first(parts, "level"),
        unit=turkish_token(request.address, "(?:daire|d)") or first(parts, "unit"),
        postalCode=first(parts, "postcode"),
        components=components,
    )
