# cloudflare-scalajs-template

Small self-contained project to demonstrate using Cloudflare Pages and Functions with Scala.js.

See my blog post about this topic: https://blog.indoorvivants.com/2022-02-14-cloudflare-functions-with-scalajs

## usage

- Live version of the app is deployed here: https://cloudflare-scalajs-template.pages.dev
- Locally, you need to install Wrangler and run `wrangler pages dev . --kv=SQUARE`, and in a separate terminal `sbt "~buildWorkers"`
- You can see `./deploy.sh` script for the steps Cloudflare does to deploy this

![2022-02-24 18 13 42](https://user-images.githubusercontent.com/1052965/155583475-2a777807-40df-4a09-97a8-967cf7dd2f5f.gif)
