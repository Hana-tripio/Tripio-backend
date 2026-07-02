# 지도 API

지도 화면의 지역 버블과 장소 핀을 제공한다.

모든 응답은 공통 `ApiResponse<T>` 형식을 사용한다.

## 지역 버블 조회

```http
GET /api/map/regions
GET /api/map/regions?parentRegionId=10
```

`parentRegionId`가 없으면 최상위 지역을 반환하고, 있으면 해당 지역의 하위 지역을 반환한다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "regions": [
      {
        "regionId": 10,
        "name": "충청권",
        "regionType": "PROVINCE",
        "latitude": 36.6424000,
        "longitude": 127.4890000,
        "perScore": 44,
        "localContributionBaseScore": 70,
        "hasChildren": true
      }
    ]
  }
}
```

## 지역 장소 핀 조회

```http
GET /api/map/regions/{regionId}/places
```

특정 지역에 속한 장소를 지도 핀 표시용으로 반환한다. 장소는 코어 스팟이 먼저 오고, 같은 그룹에서는 이름순으로 정렬된다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "places": [
      {
        "placeId": 100,
        "name": "공주산성시장",
        "address": "충남 공주시 산성시장",
        "latitude": 36.4550000,
        "longitude": 127.1230000,
        "category": "MARKET",
        "isLocal": true,
        "isCoreSpot": true,
        "imageUrl": "https://example.com/place.jpg",
        "estimatedCost": 15000
      }
    ]
  }
}
```

존재하지 않는 `parentRegionId` 또는 `regionId`는 `COMMON404`를 반환한다.
