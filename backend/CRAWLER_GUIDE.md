# 에브리타임 셀레니움 크롤러 사용 가이드

이 문서는 UniWiki 팀원들이 로컬 환경에서 에브리타임 게시글 크롤링 API를 사용하는 방법을 안내합니다.

## 핵심 요약

에브리타임 로그인과 수집은 로컬 PC에서 수행하고, 수집 결과만 토큰으로 보호된 운영 API에 전송합니다. 운영 서버에서는 에브리타임 세션 쿠키를 사용하지 않습니다.

---

##  사용 방법 (최초 1회)

1. 로컬 환경 변수 설정

   ```text
   EVERYTIME_CRAWL_ENABLED=true
   EVERYTIME_HEADLESS=false
   EVERYTIME_UPLOAD_URL=https://<운영-백엔드>/api/admin/crawl/everytime/lecture/import
   EVERYTIME_UPLOAD_TOKEN=<운영 EVERYTIME_IMPORT_TOKEN과 같은 값>
   ```

   토큰은 `.env`나 Git에 저장하지 말고 실행 환경에만 설정합니다.

2. 백엔드 서버 실행
   - 백엔드 서버(`UniwikiApplication`)를 실행합니다.
   - 처음 실행 시 `selenium-java` 관련 라이브러리가 다운로드되므로 약간의 시간이 걸릴 수 있습니다.
   - 일회성 수집 모드는 로컬 메모리 DB를 사용할 수 있으며, 실제 결과는 운영 import API에만 저장됩니다.

   ```powershell
   $env:DB_URL='jdbc:h2:mem:collector;MODE=MySQL;DB_CLOSE_DELAY=-1'
   $env:DB_USERNAME='sa'
   $env:DB_PASSWORD=''
   $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.h2.Driver'
   $env:SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT='org.hibernate.dialect.H2Dialect'
   $env:SPRING_JPA_HIBERNATE_DDL_AUTO='create-drop'
   $env:EVERYTIME_RUN_ON_STARTUP='true'
   .\gradlew.bat bootRun
   ```

3. Swagger에서 API 호출
   - `http://localhost:8080/swagger-ui/index.html` 에 접속합니다.
   - `POST /api/admin/crawl/everytime` API를 찾습니다.
   - Request Body에 아래 양식을 넣고 **Execute(실행)** 버튼을 누릅니다. (이제 `etsid` 값은 필요 없습니다!)
   
   ```json
   {
     "boardUrl": "https://everytime.kr/370445",
     "targetTable": "Question",
     "categoryId": 1
   }
   ```

4. **크롬 창 팝업 및 수동 로그인**
   - API를 실행하면 모니터 화면에 **'새로운 크롬 브라우저 창'**이 자동으로 팝업됩니다.
   - 에브리타임 메인 화면이 뜨면 당황하지 마시고, **60초 안에 본인의 에타 아이디와 비밀번호를 입력하고 로그인**을 완료해 주세요.
   - *주의: 60초 안에 로그인을 완료하지 않으면 타임아웃 에러가 발생합니다.*

5. **자동 크롤링 및 운영 업로드**
   - 로그인이 완료되는 즉시, 봇이 화면을 감지하고 지정한 게시판(`boardUrl`)으로 번개처럼 이동합니다.
   - 강의평을 수집하면 운영 import API가 중복을 검사한 뒤 `raw_lecture_evaluations`에 저장합니다.
   - 운영 서버의 기존 워크플로가 개인정보 마스킹, 품질 검사, 위키 초안 생성을 이어서 수행합니다.

---

## 그 다음부터는? 

Chrome 프로필은 로컬 임시 디렉터리의 `EverytimeChromeProfile`에 저장됩니다. 운영 서버로 로그인 정보가 전송되지 않습니다.

- 다음번 API 호출부터는 아이디/비밀번호를 입력하실 필요가 전혀 없습니다.
- API를 호출하면 크롬 창이 뜨자마자 "1초 만에" 자동으로 글을 긁어오고 종료됩니다! 

## 주의 사항
- 봇이 구동되는 동안에는 팝업된 크롬 창을 강제로 끄거나, 다른 사이트로 이동하지 마세요. 봇이 길을 잃습니다.
- 크롤링하려는 게시판 주소(`boardUrl`)는 본인 계정으로 접근 권한이 있는 게시판이어야 합니다. (새내기 게시판, 졸업생 게시판 등 본인 권한이 없는 곳은 에러가 발생할 수 있습니다.)

## 소프트웨어학과 강의평 일괄 수집

`POST /api/admin/crawl/everytime/lecture/batch`는
`ai/data/normalized/sejong/software-course-schedules.json`에서 강의명·교수명 조합을 읽고,
에브리타임 검색 결과가 정확히 일치하는 강의의 평가만 저장합니다.

```json
{
  "terms": ["2026-1", "2026-2"],
  "maxCourseProfessorPairs": 10,
  "startPage": 1,
  "endPage": 3,
  "requestDelayMillis": 1500
}
```

- `terms`를 생략하면 2024-1~2026-2 전체 데이터를 사용합니다.
- 첫 시험에서는 `maxCourseProfessorPairs`를 1~3으로 지정하는 것을 권장합니다.
- 강의명과 교수명이 모두 일치해야 `MATCHED`로 처리합니다.
- 결과가 없거나 여러 개인 경우 각각 `NOT_FOUND`, `AMBIGUOUS`로 반환하며 저장하지 않습니다.
- 같은 출처·강의명·교수명·본문의 평가는 다시 저장하지 않습니다.
- 시간표 경로가 다른 환경에서는 `EVERYTIME_COURSE_DATA_PATH` 환경 변수로 지정합니다.
- 실행 중에는 같은 `EverytimeChromeProfile`을 사용하는 다른 Chrome 크롤러를 실행하지 마세요.

## 강의평 자동 처리 워크플로

DB에 저장된 강의평은 기본 10초 간격으로 다음 과정을 거칩니다.

1. 별점과 최소 정보량을 검사한다.
2. 이메일, 전화번호, 학번, 외부 링크를 마스킹한다.
3. 모욕적 표현이나 저품질 리뷰를 제외한다.
4. 통과한 리뷰를 강의명·교수명별 위키 `PENDING` 초안으로 집계한다.
5. 관리자가 초안을 검토해 `APPROVED`로 변경하면 기존 벡터 동기화 큐가 AI 서비스에 반영한다.

즉시 수동 실행하려면 `POST /api/admin/crawl/everytime/lecture/process`를 호출합니다.
처리 결과와 제외 사유는 `GET /api/admin/crawl/everytime/lecture`에서 확인할 수 있습니다.

기존 DB에는 `database/migration-lecture-review-workflow.sql`을 한 번 적용해야 합니다.
초안 작성자 ID와 카테고리는 각각 `LECTURE_REVIEW_AUTHOR_ID`,
`LECTURE_REVIEW_CATEGORY_NAME` 환경 변수로 변경할 수 있습니다.

## 운영 서버 설정

운영 백엔드에는 다음 값을 설정합니다.

```text
EVERYTIME_CRAWL_ENABLED=false
EVERYTIME_IMPORT_TOKEN=<충분히 긴 임의의 비밀값>
EVERYTIME_SESSION_COOKIES=
```

- `EVERYTIME_CRAWL_ENABLED=false`는 운영 컨테이너에서 브라우저 크롤링을 차단합니다.
- import API는 `X-Crawler-Token` 헤더가 운영 토큰과 일치할 때만 데이터를 받습니다.
- 같은 출처 URL, 강의명, 교수명, 본문 조합은 중복 저장하지 않습니다.
