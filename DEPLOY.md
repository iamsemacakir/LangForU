# Deploying LangForU for free

LangForU runs on four free services. None of them needs a credit card, and you never have to manage a server yourself.

| Part | Service | Free limits |
|------|---------|-------------|
| Website (Java app) | [Render](https://render.com) | 512 MB RAM, 750 hours/month (enough for one site running all month), 5 GB outgoing traffic/month |
| Database (PostgreSQL) | [Neon](https://neon.com) | 1 GB storage, permanent (does not expire) |
| Emails | [Brevo](https://www.brevo.com) | 300 emails/day |
| Sevi (AI assistant) | [Google AI Studio / Gemini API](https://aistudio.google.com) | Free tier with daily request limits |
| Keep the site awake | [cron-job.org](https://cron-job.org) | Free |

Do the steps in this order. Write each value down as you go; you will paste them all into Render in step 6.

---

## 1. Database: Neon

1. Sign up at [neon.com](https://neon.com) and create a project named `langforu`. Choose a region in Europe (e.g. Frankfurt).
2. Open **Connect** on the project dashboard. Turn **Connection pooling off** (use the direct connection).
3. You will see a connection string like this:

   ```
   postgresql://USER:PASSWORD@HOST/neondb?sslmode=require
   ```

4. Split it into three values for Spring Boot:

   | Setting | Value |
   |---------|-------|
   | `DB_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` |
   | `DB_USERNAME` | `USER` |
   | `DB_PASSWORD` | `PASSWORD` |

   (Add `jdbc:` in front and move the user and password out of the address.)

The tables are created automatically the first time the app starts.

## 2. Emails: Brevo

Render's free plan blocks the normal email ports (25, 465, 587), so Gmail cannot be used there. Brevo works on port **2525**, which is already set in `render.yaml`.

1. Sign up at [brevo.com](https://www.brevo.com) (free plan).
2. Add and verify the address you want to send from (for example `langforu.softdev@gmail.com`) under **Senders**. This is `MAIL_FROM`.
3. Go to **SMTP & API → SMTP**. If SMTP is not enabled yet, Brevo asks you to request activation once.
4. Copy the **SMTP login** (it looks like `xxxxxxx@smtp-brevo.com`). This is `MAIL_USERNAME`.
5. Click **Generate a new SMTP key** and copy it. This is `MAIL_PASSWORD`.

Tip: emails sent "from" a Gmail address through another service can land in Spam. Ask your first users to check their Spam folder. A custom domain fixes this later.

## 3. Sevi: free Gemini API key

1. Go to [aistudio.google.com](https://aistudio.google.com) and sign in with a Google account.
2. Click **Get API key → Create API key** and copy it. This is `OPENAI_API_KEY`.

The app talks to Gemini through its OpenAI-compatible address, so no code change is needed (`OPENAI_API_URL` and `OPENAI_API_MODEL` are already set in `render.yaml`). If Sevi ever reports a "model not found" error, replace `OPENAI_API_MODEL` in Render with the current Flash model name shown in AI Studio.

Note: on the free tier, Google may use prompts to improve its products. Mention this on the Terms page.

## 4. Admin password

Choose a strong password for the admin account (`langforu.softdev@gmail.com`). This is `ADMIN_PASSWORD`. The admin is created automatically on the first start.

Do **not** reuse the old `Admin123` password: it is visible in the public Git history.

## 5. Encryption salt

Choose 16 characters using only `0-9` and `a-f` (for example, type them yourself or run `openssl rand -hex 8`). This is `ENCRYPTION_SALT`. Render generates `ENCRYPTION_KEY` for you.

**Never change the key or salt after launch**, otherwise saved encrypted data (EGN on certificates) can no longer be read.

## 6. Website: Render

1. Sign up at [render.com](https://render.com) with your GitHub account.
2. Click **New → Blueprint** and select the `LangForU` repository. Render reads `render.yaml` from the repo.
3. Fill in the values it asks for:
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `OPENAI_API_KEY`, `ADMIN_PASSWORD`, `ENCRYPTION_SALT`.
4. Click **Apply**. The first build takes several minutes.
5. When it is live, your site is at an address like `https://langforu.onrender.com` (shown at the top of the service page).

Links in emails (registration confirmation, etc.) use this address automatically. If you later add your own domain, set `APP_BASE_URL` to it in **Environment**.

Every push to the `main` branch redeploys the site automatically.

## 7. Keep the site awake: cron-job.org

On the free plan, Render puts the site to sleep after 15 minutes without visitors, and the next visitor waits about a minute. A free "ping" every 10 minutes prevents this.

1. Sign up at [cron-job.org](https://cron-job.org).
2. Click **Create cronjob**.
3. URL: `https://YOUR-SITE.onrender.com/ping` (your own address + `/ping`).
4. Schedule: **every 10 minutes**. Save.

Use `/ping`, not the homepage (it loads data from the database) and not `/robots.txt` (Render answers that itself while the site sleeps, so it never wakes it).

One always-on site uses about 744 of the 750 free hours per month, so this works for **one** site only.

## 8. Check that everything works

- `https://YOUR-SITE.onrender.com/ping` shows `ok`.
- Register a test account: the confirmation email arrives (check Spam) and the link opens your site, not `localhost`.
- Log in as admin with `langforu.softdev@gmail.com` and your `ADMIN_PASSWORD`.
- Ask Sevi a question.
- Pick an avatar on the profile page and download a certificate.

If something fails, open the service in Render and look at **Logs**.

---

## Running locally

1. Copy `.env.example` to `.env` in the project folder and fill in the values (`.env` is ignored by Git).
2. Run `mvn spring-boot:run` and open http://localhost:8080.

## Good to know

- Render may restart free services occasionally; in that case a visitor can see a one-minute wait.
- The database sleeps after 5 minutes without queries and wakes up in well under a second. The `/ping` page does not touch the database, so it does not use up Neon's free hours.
- Watch the 5 GB monthly traffic limit on Render (Dashboard → Billing/Usage) if the site gets popular.
