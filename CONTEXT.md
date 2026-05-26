# Jira Miner

Jira Miner exports Jira project data for downstream analysis. It supports both Cloud and Server deployments while aiming to present the same exported domain data regardless of deployment-specific API differences.

## Language

**Cloud**:
A Jira deployment whose server info reports `deploymentType` as `Cloud`.
_Avoid_: SaaS, hosted Jira

**Server**:
A Jira deployment whose server info reports `deploymentType` as `Server`.
_Avoid_: DC, Data Center, on-prem

**Basic export**:
An export that includes issue and status data without issue changelogs or comments.
_Avoid_: plain export, lightweight export

**Detailed export**:
An export that includes issue changelogs and comments in addition to the basic export data.
_Avoid_: full export, enriched export

**Qualified user ID**:
A deployment-qualified identifier for a Jira user, formatted as `cloud:{accountId}` or `server:{key}`.
_Avoid_: stable user ID, raw user key

**Loaded comments**:
Comments that have already been attached to an issue object and are treated as good enough for export.
_Avoid_: refreshed comments, guaranteed-complete comments

**Full changelog**:
An issue changelog that is present and complete enough for detailed export, whether returned inline or completed through follow-up requests.
_Avoid_: partial history, raw changelog page

## Relationships

- A **Basic export** contains issues and statuses, but not issue comments or full change history
- A **Detailed export** contains all **Basic export** data plus **Loaded comments** and a **Full changelog** for each exported issue
- A **Qualified user ID** is derived differently in **Cloud** and **Server**, but serves the same exported role
- **Cloud** and **Server** may use different Jira API paths while producing the same export concepts

## Example dialogue

> **Dev:** "For a **Detailed export**, do **Cloud** and **Server** need to return changelogs the same way?"
> **Domain expert:** "No. They can use different API behavior, as long as the exported issue ends up with a **Full changelog** in both cases."
>
> **Dev:** "If an issue already has comments from cache, should we reload them?"
> **Domain expert:** "No — once comments are **Loaded comments**, they are treated as good enough for export."

## Flagged ambiguities

- "DC" was used informally to mean the existing non-Cloud path — resolved: the canonical deployment terms are **Cloud** and **Server** because those are the detected values
