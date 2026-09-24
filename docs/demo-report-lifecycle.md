# Demo report lifecycle

With the `demo` profile and `app.demo-data.enabled=true`, the seeder prepares a
completed learning period for the existing demo teacher and Alex. The dedicated
student program is **Python: практический проект**. Its five attended, 60-minute
sessions cross the program's 300-minute report threshold through
`LearningPeriodService`. No report is seeded for this period.

An authenticated demo teacher can find it through the existing REST API:

1. `GET /api/v1/teacher/dashboard`
2. Find an `attentionItems` entry with `type` equal to
   `LEARNING_PERIOD_REPORT_MISSING`, `studentId` equal to
   `d0000000-0000-4000-8000-000000000004`, and
   `navigation.studentProgramId` equal to
   `d1000000-0000-4000-8000-000000000006`.
3. Read `navigation.learningPeriodId` (the seeded value is
   `da000000-0000-4000-8000-000000000004`). Initially,
   `navigation.reportId` is `null`.
4. `POST /api/v1/teacher/reports` with the session's CSRF header and body:

   ```json
   {
     "studentProgramId": "d1000000-0000-4000-8000-000000000006",
     "learningPeriodId": "da000000-0000-4000-8000-000000000004"
   }
   ```

The response is a `DRAFT` report. E2E can update it and publish it with
`POST /api/v1/teacher/reports/{reportId}/publish`, passing the report's current
`version` in the JSON body.
