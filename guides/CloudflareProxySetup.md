# Cloudflare Proxy Setup Guide

This guide walks you through creating your own **free** Cloudflare Worker that acts as a resource pack proxy for Spoofified.

## Why use a proxy?

When a server sends you a resource pack, your game normally connects **directly** to the URL the server gave it. That means whoever hosts the pack can see your **real IP address** (and your Java version via the User-Agent).

With a proxy, your client asks *your* Cloudflare Worker to download the pack instead. The pack host only sees Cloudflare's IP, not yours.

```
Without proxy:   You  ───────────────────────►  Pack host   (sees your IP)
With proxy:      You  ──►  Your Worker  ──────►  Pack host   (sees Cloudflare's IP)
```

**What you need:**

- A free Cloudflare account (no credit card required)
- About 5 minutes
- Spoofified installed

---

## Table of Contents

1. [Create a Cloudflare account](#1-create-a-cloudflare-account)
2. [Create a Worker](#2-create-a-worker)
3. [Paste the proxy code](#3-paste-the-proxy-code)
4. [Deploy the Worker](#4-deploy-the-worker)
5. [Find your Worker URL](#5-find-your-worker-url)
6. [Test your proxy](#6-test-your-proxy)
7. [Add the URL to Spoofified](#7-add-the-url-to-spoofified)
8. [Optional: lock down your proxy](#8-optional-lock-down-your-proxy)
9. [Troubleshooting](#9-troubleshooting)
10. [Free plan limits](#10-free-plan-limits)

---

## 1. Create a Cloudflare account

1. Go to **https://dash.cloudflare.com/sign-up**
2. Enter your email and a password, then click **Sign up**.
3. Check your inbox and **verify your email address**.
4. Log in at **https://dash.cloudflare.com**.

You do **not** need to add a domain or a payment method.

---

## 2. Create a Worker

1. In the left sidebar of the dashboard, click **Workers & Pages** (it may be under *Compute* in newer layouts).
2. Click the **Create** button (or **Create application**).
3. Choose **Start with Hello World!** (or **Create Worker**).
4. Give your Worker a name, for example `mc-pack-proxy`.
    - This name becomes part of your URL, so pick something you're happy with.
    - Use only lowercase letters, numbers and dashes.
5. Click **Deploy**.

Cloudflare will deploy a default "Hello World" Worker. We'll replace its code in the next step.

---

## 3. Paste the proxy code

1. After deploying, click **Edit code** (top right of the success page).
    - If you left that page, open **Workers & Pages**, click your Worker's name, then click **Edit code**.
2. In the code editor, **select all** the existing code (`Ctrl + A` / `Cmd + A`) and **delete it**.
3. Paste in the following code:

```javascript
export default {
  async fetch(request) {
    const url = new URL(request.url);
    const targetUrl = url.searchParams.get("url");

    if (!targetUrl) {
      return new Response("Missing 'url' parameter", { status: 400 });
    }

    try {
      const proxyResponse = await fetch(targetUrl, {
        method: "GET",
        redirect: "follow",
        headers: {
          // Generic Java HTTP client used by a Minecraft instance.
          "User-Agent": "Java-http-client/25.0.1",

          // Commonly appropriate for Minecraft asset/resource downloads.
          "Accept": "*/*",
          "Accept-Encoding": "gzip, deflate",
          "Connection": "keep-alive"
        }
      });

      return new Response(proxyResponse.body, {
        status: proxyResponse.status,
        headers: {
          "Content-Type":
            proxyResponse.headers.get("Content-Type") ||
            "application/zip",

          "Content-Disposition":
            proxyResponse.headers.get("Content-Disposition") ||
            "attachment; filename=pack.zip"
        }
      });
    } catch (error) {
      return new Response(
        "Proxy error: " +
          (error instanceof Error ? error.message : String(error)),
        { status: 500 }
      );
    }
  }
};
```



### What does this code do?

| Part                                      | Purpose                                                                                   |
|-------------------------------------------|-------------------------------------------------------------------------------------------|
| `url.searchParams.get("url")`             | Reads the pack address from `?url=...` in the request                                     |
| `fetch(targetUrl, ...)`                   | Downloads the pack **from Cloudflare's servers**, not your PC                             |
| `"User-Agent": "Java-http-client/25.0.1"` | Makes the request look like a generic Java client instead of revealing anything about you |
| `proxyResponse.body`                      | Streams the file straight back to you without loading it all in memory                    |
| `Content-Type` / `Content-Disposition`    | Passes the file type through, with a sensible `pack.zip` fallback                         |
| `catch`                                   | Returns a readable error if the download fails                                            |

---

## 4. Deploy the Worker

1. Click the **Deploy** button in the top right of the editor.
2. Wait for the confirmation message that the new version is live.


> ⚠️ If you only click **Save** or edit the code without deploying, your changes are **not live**. Always press **Deploy**.

---

## 5. Find your Worker URL

Your Worker's address looks like this:

```
https://<worker-name>.<your-subdomain>.workers.dev
```

For example: `https://mc-pack-proxy.myname.workers.dev`

To find it:

1. Go to **Workers & Pages**.
2. Click your Worker.
3. Open the **Settings** tab, then **Domains & Routes**.
4. Copy the `workers.dev` URL. (It's also shown at the top of the Worker's overview page.)

**Important:** copy the URL **without a trailing slash** and **without** any `?url=` part.

- ✅ `https://mc-pack-proxy.myname.workers.dev`
- ❌ `https://mc-pack-proxy.myname.workers.dev/`
- ❌ `https://mc-pack-proxy.myname.workers.dev?url=`

Spoofified adds the `?url=...` part itself.

---

## 6. Test your proxy

Before using it in-game, make sure it works.

**Test 1: missing parameter.** Open your Worker URL in your browser with nothing added:

```
https://mc-pack-proxy.myname.workers.dev
```

You should see:

```
Missing 'url' parameter
```

That means the Worker is running correctly.

**Test 2: real download.** Open this in your browser, replacing the Worker address with yours:

```
https://mc-pack-proxy.myname.workers.dev/?url=https%3A%2F%2Fexample.com%2Fpack.zip
```

Replace `https%3A%2F%2Fexample.com%2Fpack.zip` with a real resource pack link (URL-encoded). If a file downloads, the proxy works.


> 💡 **Tip:** Don't know how to URL-encode a link? Paste it into any "URL encoder" website. Spoofified does this automatically in-game, so it's only needed for manual testing.

---

## 7. Add the URL to Spoofified

1. Launch Minecraft with Spoofified installed (or open its config screen / config file).
2. Find the resource pack proxy options:
    - **Proxy Resource Packs**: turn **ON**
    - **Proxy URL**: paste your Worker URL (no trailing slash)
3. Save the config.

Example:

```
Proxy Resource Packs: ON
Proxy URL: https://mc-pack-proxy.myname.workers.dev
```

> 📝 The exact option names may differ slightly depending on your Spoofified version. Look for anything mentioning *proxy*.

### How it works in-game

When a server sends a pack such as `https://example.com/pack.zip`, Spoofified rewrites it to:

```
https://mc-pack-proxy.myname.workers.dev?url=https%3A%2F%2Fexample.com%2Fpack.zip
```

Your client downloads from your Worker, and your Worker downloads from the real host.

Spoofified also has a **Block Local Packs** option. When it's on, packs pointing at local/private addresses (like `192.168.x.x` or `localhost`) are blocked instead of being proxied. This protects you from servers trying to scan your local network.



---

## 8. Optional: lock down your proxy

As written, your Worker will fetch **any** URL for **anyone** who knows its address. Nobody will find it by accident, but it's good practice to restrict it, because:

- Strangers could use your Worker as a free proxy.
- Heavy use by others could burn through your free request quota.

### Option A: add a secret token (recommended)

Edit your Worker code and add a check at the top of `fetch`:

```javascript
export default {
  async fetch(request) {
    const url = new URL(request.url);

    // Require ?key=YOUR_SECRET on every request
    if (url.searchParams.get("key") !== "CHANGE_ME_TO_A_LONG_RANDOM_STRING") {
      return new Response("Forbidden", { status: 403 });
    }

    const targetUrl = url.searchParams.get("url");
    // ... rest of the code stays the same ...
```

Then set your Proxy URL in Spoofified to include the key:

```
https://mc-pack-proxy.myname.workers.dev/?key=CHANGE_ME_TO_A_LONG_RANDOM_STRING
```

> ⚠️ This only works if your Spoofified version appends `&url=` correctly when your Proxy URL already contains a `?`. By default it appends `?url=`, so test this carefully. If it doesn't work, use Option B instead.

### Option B: only allow http/https targets

Add this after the `targetUrl` check:

```javascript
let parsed;
try {
  parsed = new URL(targetUrl);
} catch {
  return new Response("Invalid URL", { status: 400 });
}
if (parsed.protocol !== "https:" && parsed.protocol !== "http:") {
  return new Response("Unsupported protocol", { status: 400 });
}
```

### Option C: rate limiting

In the Cloudflare dashboard you can add rate-limiting rules (under **Security → WAF → Rate limiting rules**) if you have a domain attached to your Worker.



Remember to click **Deploy** again after any change.

---

## 9. Troubleshooting

| Problem                                          | Likely cause                                               | Fix                                                                                        |
|--------------------------------------------------|------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `Missing 'url' parameter` in the game or browser | The request didn't include `?url=`                         | Check that your Proxy URL has no `?url=` already and that Spoofified's proxy option is ON  |
| `Proxy error: ...`                               | The Worker couldn't reach the pack host                    | Check the pack link works in a normal browser; the host may be down or blocking Cloudflare |
| Pack never downloads / hangs                     | Wrong Worker URL                                           | Re-copy the URL from the dashboard; make sure it starts with `https://`                    |
| `Error 1101` / Worker threw exception            | A code typo                                                | Open **Edit code**, re-paste the original code, and **Deploy**                             |
| Changes didn't apply                             | You didn't deploy                                          | Click **Deploy** again                                                                     |
| `403 Forbidden`                                  | You added a secret key but it doesn't match                | Check the key in the Worker and in Spoofified match exactly                                |
| `1015` / rate limited                            | Too many requests                                          | You may have hit the free limit (see below) or Cloudflare rate-limited you                 |
| Pack blocked with an "Attack Blocked" message    | The server tried to send a local/private address           | This is intended. **Block Local Packs** is protecting you                                  |
| Some hosts refuse the download                   | Some hosts block Cloudflare IPs or require special headers | This can't be fixed from your side, but you can try disabling the proxy for that server    |

### Viewing logs

1. Open your Worker in the dashboard.
2. Go to the **Observability** or **Logs** tab.
3. Click **Begin log stream** and reproduce the problem in-game.



---

## 10. Free plan limits

Cloudflare's free Workers plan is generous, but it has limits. Check Cloudflare's current documentation for exact numbers, since they can change.

- A daily cap on the number of requests (around 100,000 per day at the time of writing). A resource pack download is usually **one** request, so this is plenty for personal use.
- A small CPU time limit per request. Since the Worker just streams data through, it normally stays well within this.
- Very large packs work because the response is **streamed** (`proxyResponse.body`) rather than buffered, but the host must still allow Cloudflare to download from it.

If you share your Worker with friends, keep an eye on usage in the dashboard under **Workers & Pages → your Worker → Metrics**.


---

## Quick Summary

1. Make a free Cloudflare account.
2. **Workers & Pages → Create → Hello World → Deploy**.
3. **Edit code**, paste the proxy script, then **Deploy**.
4. Copy your `https://<name>.<subdomain>.workers.dev` URL (no trailing slash).
5. In Spoofified, turn on **Proxy Resource Packs** and paste the URL into **Proxy URL**.
6. Join a server and enjoy not leaking your IP to pack hosts. 🎉

---

## Privacy notes

- The proxy hides your IP from the **pack host**, but **Cloudflare** can still see requests passing through your own Worker. You're trusting your own Cloudflare account, which is usually a better deal than trusting every server's pack host.
- The **Minecraft server** you're connected to still sees your IP, since you're playing on it. The proxy only protects against the separate pack download.
- Don't share your Worker URL publicly unless you've added protection (see [section 8](#8-optional-lock-down-your-proxy)).