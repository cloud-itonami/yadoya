# Operator quickstart

What you can actually run in this repo, measured on 2026-08-27 rather than
inferred from the manifests.

Read this before `README.md`. The README describes a TinyGo App component with
nine REST endpoints, seven MCP tools and a twenty-property hotel catalog. **None
of that is in this repo** — see [What is actually here](#what-is-actually-here).
The README describes the upstream seed (`etzhayyim-root/60-apps/etzhayyim-project-yadoya`,
recorded in `migration.edn`), not the tree you have checked out.

## TL;DR

```sh
cd kotoba
npm_config_userconfig=/dev/null npm install   # see "The install gotcha" — plain `npm install` fails
npx tsc --noEmit                              # typecheck
npx vitest run                                # 12 tests
```

## What is actually here

| Path | What it is | Runs today? |
|---|---|---|
| `kotoba/` | TypeScript library: listings + bookings on AT PDS records, on-chain USDC settlement via TitheRouter (10% tithe). 718 lines incl. tests. | **Yes** — walked below |
| `worker/` | Cloudflare Worker (`etzhayyim-yadoya`) serving `yadoya.etzhayyim.com`, SvelteKit edge BFF. `main` points at a `.svelte-kit/` build output. | Not walked in this iteration |
| `appview/yadoya-ui-b7r4n2xq/svelte/` | Svelte 5 + Vite UI. | **No** — see below |

`appview` cannot be installed from this repo at all. It declares
`"@etzhayyim/design-system": "workspace:*"`, but the repo has no workspace root
(no top-level `package.json`, no `pnpm-workspace.yaml`), so the sibling package
does not exist here:

```
ERR_PNPM_WORKSPACE_PKG_NOT_FOUND  In : "@etzhayyim/design-system@workspace:*" is in the
  dependencies but no package named "@etzhayyim/design-system" is present in the workspace
```

(one line in the original; wrapped here)

That is an artifact of the extraction (`chore: extract yadoya app from root`) —
the workspace sibling did not come across with it. Fixing it means either
vendoring the design system or restoring a workspace root; neither is done here.

## The install gotcha

A bare `npm install` in `kotoba/` fails if your user-level `~/.npmrc` carries an
`allow-scripts[]` entry. It is worth stating the error exactly, because it names
a flag you never passed:

```
npm error code EALLOWSCRIPTS
npm error --allow-scripts is not allowed in project-scoped installs.
npm error Add the entries to the "allowScripts" field in package.json, or to .npmrc, instead.
```

**Cause (measured, not guessed).** `@etzhayyim/sdk` is a git dependency with
`"prepare": "tsc"`, so npm must run a script to prepare it. npm spawns a nested
install to do that, and the nested install inherits `allow-scripts[]` from the
user-level `~/.npmrc` as a CLI flag — which npm 11.19.0 then rejects in a
project-scoped install. A `~/.npmrc` containing nothing but

```
allow-scripts[]=@anthropic-ai/claude-code
```

is sufficient to reproduce it. Neutralising the user config fixes it:

```sh
npm_config_userconfig=/dev/null npm install --no-audit --no-fund
# added 135 packages in 4m
```

Expect minutes, not seconds, and expect the number to move: the same command on
the same tree took 7m and 4m on two runs an hour apart. Most of it is `tsc`
preparing the git dependencies (below), so it tracks whatever else the machine
is doing.

Two things this is *not*:

- **Not a repo defect.** With the user config neutralised the declared
  dependencies install unchanged.
- **Not fixable by switching package manager.** `pnpm install` fails at the same
  root cause: after you allow the build (`onlyBuiltDependencies: ["@etzhayyim/sdk"]`
  in `pnpm-workspace.yaml`) pnpm delegates preparation to `npm install`, which
  hits the identical `EALLOWSCRIPTS`.

Measured on Node v26.7.0 / npm 11.19.0 / pnpm 10.26.2, on 2026-08-27. Which npm
versions besides 11.19.0 behave this way was **not** measured; do not read the
above as a claim about 11.18 and below. If your `~/.npmrc` has no
`allow-scripts[]` entry, a bare `npm install` may well work — that case was not
measured either.

## Typecheck

```sh
cd kotoba
npx tsc --noEmit     # exit 0, no output
```

## Test

```sh
cd kotoba
npx vitest run
```

Observed:

```
 RUN  v4.1.11 <your-checkout>/kotoba

 Test Files  1 passed (1)
      Tests  12 passed (12)
   Duration  1.97s
```

(`Duration` tracks machine load — 1.97 s and 4.03 s were both observed on the
same tree minutes apart. The counts are the part to read.)

All twelve run against `MockEtzhayyim`, not a live PDS — no network and no chain
access is required. They cover the date helpers and the tithe split; the listing
path (create/get/list, idempotent re-create, non-positive price rejected); and
the booking path (nights and total computed, checkout-before-checkin rejected,
over-capacity rejected, missing listing → `listingNotFound`, settlement splits
the tithe and confirms the booking, and settling twice is refused).

### This command discriminates

A green suite is only worth reading if a broken repo turns it red. Verified by
mutating the constitutional 10% split in `src/tithe.ts`:

```
-export const TITHE_PERMILLE = 100n;
+export const TITHE_PERMILLE = 90n;
```

```
AssertionError: expected 27000000n to be 30000000n
 Test Files  1 failed (1)
      Tests  2 failed | 10 passed (12)     # exit 1
```

Restoring the constant returns 12/12 and exit 0. Vitest transforms `src/`
directly, so there is no build step to forget: if you change settlement maths
and the suite stays green, the honest reading is that your change is not
covered — write the test, do not assume it was safe.

## Known gaps

- `README.md` documents a different system (TinyGo component, `/api/*` REST
  surface, MCP tools, 20-hotel catalog). None of it exists in this tree. It has
  not been rewritten here because the correct replacement text requires deciding
  what this repo is *for*, which is an owner call, not a documentation edit.
- `MIGRATION-TODO.md` still lists the Charter §2(a)-(h) audit as unchecked. The
  automated codemod scan found no violations; the manual review is outstanding.
- No lockfile is tracked. `npm install` resolves eight git dependencies fresh
  every time — `@etzhayyim/sdk`, `@etzhayyim/sdk-mock` and six `kotoba-lang`
  packages, seven of which run `prepare: tsc` — hence the minutes.
  Whether to commit a lockfile is an open decision, so this iteration did not
  make it; `npm install` will leave `kotoba/package-lock.json` untracked.
- `worker/` was not built or deployed in this iteration, so nothing here says
  whether `yadoya.etzhayyim.com` currently serves this tree.
