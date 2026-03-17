# AI Guidance University Data Engine Plan

## Current implementation upgrades in this PR
1. Use truthful analysis labels (`analysisModeLabel`, `sourceTrustLabel`, `confidenceLevel`, `sourceBackedAnalysis`) returned by backend and rendered by UI.
2. Keep Gemini grounding strict: APS values are forbidden unless explicitly in fetched context, and due dates/subject minimums must never be invented.
3. Expand source discovery for official university pages by combining seed URLs + discovered internal programme/admissions pages before fetch.
4. Add recommendation-level transparency (`recommendationBasis`: `SOURCE_VERIFIED` or `PROFILE_ONLY`).

## Proposed production pipeline (next stage)
1. **Intent intake**
   - Normalize `targetProgram`, `careerInterest`, and profile skill/interest tokens.
2. **University source selection**
   - Rank official domains by relevance and historical success rate.
   - Select admission + programme + faculty pages first.
3. **Retrieval and extraction**
   - Fetch HTML with allowlist/domain guardrails.
   - Extract structured fields per programme:
     - programmeName
     - university
     - admissionRequirements
     - mathematicsRequirement
     - englishRequirement
     - qualificationType
     - notes
4. **Context assembly**
   - Build compact, source-attributed context blocks with URL + page title + extracted facts.
5. **Grounded generation**
   - Pass only profile + extracted context to Gemini.
   - Require per-item `recommendationBasis`.
6. **Trust and confidence scoring**
   - HIGH: >=60% requested pages fetched successfully and extracted requirements present.
   - MEDIUM: at least one source fetched, but partial extraction.
   - LOW: no usable source context.
7. **Observability**
   - Track per-source fetch status, parser coverage, and field-extraction completeness.

## Production TODOs
- Add a dedicated extractor layer (`UniversityProgrammeExtractionService`) with rule-based + ML-assisted parsing.
- Persist structured programme facts in database for faster reuse and freshness tracking.
- Add per-university robots/terms compliance settings and crawl throttling by domain.
- Add scheduled refresh with differential update (ETag/Last-Modified where available).
- Add analytics dashboard for source coverage, grounding ratio, and hallucination guardrail violations.
