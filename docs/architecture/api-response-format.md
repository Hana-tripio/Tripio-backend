# TRIPIO 공통 API 응답 규칙

## 문서 목적

이 문서는 TRIPIO 백엔드 API의 공통 응답 형식을 팀 전체 기준으로 확정하기 위해 작성한다.

현재 백엔드에는 이미 공통 응답 코드가 구현되어 있다.

```text
src/main/java/com/tripio/global/apiPayload/ApiResponse.java
src/main/java/com/tripio/global/apiPayload/code/BaseSuccessCode.java
src/main/java/com/tripio/global/apiPayload/code/BaseErrorCode.java
src/main/java/com/tripio/global/apiPayload/code/GeneralSuccessCode.java
src/main/java/com/tripio/global/apiPayload/code/GeneralErrorCode.java
src/main/java/com/tripio/global/apiPayload/exception/GeneralException.java
src/main/java/com/tripio/global/apiPayload/exception/handler/GlobalExceptionHandler.java
```

따라서 새 응답 클래스를 만들지 않고, 현재 구현된 `ApiResponse<T>` 구조를 공식 규칙으로 사용한다.

## 기본 응답 형식

모든 API 응답은 아래 필드를 기준으로 한다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {}
}
```

필드 의미:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `isSuccess` | boolean | 요청 성공 여부 |
| `code` | string | 성공 또는 실패 코드 |
| `message` | string | 사용자 또는 개발자가 이해할 수 있는 메시지 |
| `result` | object, array, string, number, boolean, null | 실제 응답 데이터 |

## 성공 응답

성공 응답은 `ApiResponse.onSuccess(...)`를 사용한다.

예시:

```text
@GetMapping("/api/example")
public ResponseEntity<ApiResponse<ExampleResponse>> getExample() {
    ExampleResponse response = exampleService.getExample();

    return ResponseEntity
            .status(GeneralSuccessCode.OK.getStatus())
            .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
}
```

응답 예시:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "id": 1,
    "name": "example"
  }
}
```

생성 API처럼 HTTP 상태가 `201 Created`가 필요한 경우에는 도메인별 성공 코드를 추가해서 사용한다.

```text
return ResponseEntity
        .status(ExampleSuccessCode.CREATED.getStatus())
        .body(ApiResponse.onSuccess(ExampleSuccessCode.CREATED, response));
```

## 실패 응답

실패 응답은 Controller에서 직접 만들지 않는다.

비즈니스 예외가 발생하면 `GeneralException` 또는 도메인별 예외를 던지고, `GlobalExceptionHandler`가 `ApiResponse` 실패 형식으로 변환한다.

예시:

```text
if (example == null) {
    throw new GeneralException(GeneralErrorCode.NOT_FOUND);
}
```

응답 예시:

```json
{
  "isSuccess": false,
  "code": "COMMON404",
  "message": "요청한 리소스를 찾을 수 없습니다.",
  "result": null
}
```

## 검증 실패 응답

`@Valid` 검증 실패는 `GlobalExceptionHandler`에서 처리한다.

응답 예시:

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "입력값 검증에 실패했습니다.",
  "result": {
    "email": "이메일 형식이 올바르지 않습니다.",
    "password": "비밀번호는 8자 이상이어야 합니다."
  }
}
```

## 도메인별 에러 코드 작성 규칙

공통 에러는 `GeneralErrorCode`를 사용한다.

도메인별 에러가 필요하면 `BaseErrorCode`를 구현한 enum을 각 도메인 패키지에 만든다.

예시 위치:

```text
src/main/java/com/tripio/etf/type/EtfErrorCode.java
src/main/java/com/tripio/design/type/DesignErrorCode.java
src/main/java/com/tripio/reward/type/RewardErrorCode.java
```

예시:

```text
@Getter
@AllArgsConstructor
public enum EtfErrorCode implements BaseErrorCode {

    ETF_NOT_FOUND(HttpStatus.NOT_FOUND, "ETF404", "ETF를 찾을 수 없습니다."),
    ETF_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ETF403", "ETF 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

코드 네이밍 규칙:

```text
COMMON400
COMMON404
ETF404
DESIGN404
USER409
REWARD400
```

권장 규칙:

- 공통 오류는 `COMMON` prefix를 사용한다.
- 도메인 오류는 도메인 prefix를 사용한다.
- HTTP 상태와 의미가 크게 어긋나지 않게 한다.
- 같은 도메인 안에서 같은 code를 중복 사용하지 않는다.

## 도메인별 성공 코드 작성 규칙

단순 조회 성공은 `GeneralSuccessCode.OK`를 사용한다.

도메인별로 메시지나 상태 코드가 필요한 경우 `BaseSuccessCode`를 구현한 enum을 만든다.

예시:

```text
@Getter
@AllArgsConstructor
public enum EtfSuccessCode implements BaseSuccessCode {

    ETF_CREATED(HttpStatus.CREATED, "ETF201", "ETF가 생성되었습니다."),
    ETF_UPDATED(HttpStatus.OK, "ETF200_1", "ETF가 수정되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## Controller 작성 규칙

Controller는 아래 원칙을 따른다.

- 성공 응답은 `ApiResponse.onSuccess(...)`로 감싼다.
- 실패 응답은 직접 만들지 않고 예외를 던진다.
- HTTP status는 success/error code의 status와 맞춘다.
- DTO를 그대로 `result`에 넣는다.
- Entity를 직접 응답하지 않는다.

권장:

```text
return ResponseEntity
        .status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
```

비권장:

```text
return ResponseEntity.ok(response);
```

비권장:

```text
return ResponseEntity
        .badRequest()
        .body(ApiResponse.onFailure(GeneralErrorCode.BAD_REQUEST, null));
```

## Frontend 처리 기준

프론트엔드는 API 응답을 아래 기준으로 처리한다.

```typescript
type ApiResponse<T> = {
  isSuccess: boolean;
  code: string;
  message: string;
  result: T;
};
```

권장 처리:

- `isSuccess === true`: `result`를 화면 데이터로 사용한다.
- `isSuccess === false`: `code`, `message`를 기준으로 에러 UI를 표시한다.
- 인증 오류는 `code` 또는 HTTP status `401`을 기준으로 로그인 흐름으로 보낸다.
- 서버 장애는 HTTP status `500` 또는 `COMMON500`을 기준으로 공통 에러 화면을 표시한다.

## 테스트 작성 기준

API 테스트는 공통 응답 필드를 검증한다.

예시:

```text
mockMvc.perform(get("/api/example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.code").value("COMMON200"))
        .andExpect(jsonPath("$.message").value("성공입니다."))
        .andExpect(jsonPath("$.result").exists());
```

실패 응답 테스트 예시:

```text
mockMvc.perform(get("/api/examples/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("ETF404"))
        .andExpect(jsonPath("$.message").value("ETF를 찾을 수 없습니다."));
```

## 현재 결론

TRIPIO 백엔드는 현재 구현된 `ApiResponse<T>` 형식을 공식 공통 응답 포맷으로 사용한다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {}
}
```

팀원이 새 API를 만들 때는 이 문서의 Controller 작성 규칙과 도메인별 code 작성 규칙을 따른다.
