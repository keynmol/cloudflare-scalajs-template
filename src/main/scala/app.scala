package app

import com.indoorvivants.cloudflare.cloudflareWorkersTypes.KVNamespace
import com.indoorvivants.cloudflare.cloudflareWorkersTypes.*
import com.indoorvivants.cloudflare.std

import scala.annotation.implicitNotFound
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.{Dynamic as JSDynamic}
import scala.scalajs.js.JSON

type Params = std.Record[String, scala.Any]

// --- FUNCTIONS

inline def STRENGTH = 5

@JSExportTopLevel(name = "onRequest", moduleID = "request_headers")
def request_headers(context: EventContext[Any, String, Params]) =
  val str = StringBuilder()
  context.request.headers.forEach { (_, value, key, _) =>
    str.append(s"Keys: $key, value: $value\n")
  }
  global.Response("hello, world. Your request comes with \n" + str.result)

@JSExportTopLevel(name = "onRequestPost", moduleID = "vote")
def vote(context: EventContext[JSDynamic, String, Params]) =
  val database =
    ColorsDB(context.env.SQUARE.asInstanceOf[KVNamespace], STRENGTH)
  val redirectUri = global.URL(context.request.url).setPathname("/").toString

  for
    formData <- context.request.formData()

    case (color, direction) <- extractVote(formData)

    _      <- database.change(color, direction)
    result <- Promise.resolve(global.Response.redirect(redirectUri))
  yield result
end vote

@JSExportTopLevel(name = "onRequestGet", moduleID = "index")
def index(context: EventContext[JSDynamic, String, Params]) =
  val database =
    ColorsDB(context.env.SQUARE.asInstanceOf[KVNamespace], STRENGTH)

  val htmlHeaders =
    ResponseInit().setHeadersVarargs(
      scala.scalajs.js.Tuple2("Content-type", "text/html")
    )

  for
    red   <- database.get(Color.Red)
    green <- database.get(Color.Green)
    blue  <- database.get(Color.Blue)
    page = renderPage(red, green, blue)
  yield global.Response(page.render, htmlHeaders)
end index

def badRequest(msg: String): Promise[Response] =
  Promise.resolve(global.Response(msg, ResponseInit().setStatus(400)))

def renderPage(red: Int, green: Int, blue: Int) =
  import scalatags.Text.all.*
  def buttonGroup(nm: String, klass: String) =
    val capitalised = nm.capitalize
    form(
      action := "/vote",
      method := "POST",
      div(
        cls     := "btn-group",
        role    := "group",
        display := "block",
        style   := "margin-bottom:10px",
        input(
          `type` := "submit",
          value  := s"-$STRENGTH",
          name   := "decrement"
        ),
        input(
          tpe   := "hidden",
          name  := "color",
          value := nm
        ),
        span(
          cls   := s"badge bg-$klass",
          style := "font-size:1.2rem;",
          capitalised
        ),
        input(
          `type` := "submit",
          value  := s"+$STRENGTH",
          name   := "increment"
        )
      )
    )
  end buttonGroup

  html(
    head(
      scalatags.Text.tags2.title("Reviews"),
      link(
        href := "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css",
        rel := "stylesheet"
      )
    ),
    body(
      div(
        cls   := "container",
        style := "padding:20px;",
        h1("Letting the internet decide what the best color is"),
        div(
          cls := "row",
          div(
            cls := "col-5",
            style := s"width:300px; height:300px; background-color: rgb($red, $green, $blue)"
          ),
          div(
            cls := "col-3",
            buttonGroup("red", "danger"),
            buttonGroup("green", "success"),
            buttonGroup("blue", "primary")
          ),
          div(
            cls := "col-4",
            p("You can decrement or increment the value of each RGB channel")
          )
        )
      )
    )
  )
end renderPage

def extractVote(fd: FormData): Promise[(Color, Dir)] =
  import Color.*
  val dir = if (fd.get("increment") != null) then Dir.Up else Dir.Down

  val color = Option(fd.get("color")).collect { case s: String =>
    Color.fromString(s)
  }.flatten

  color match
    case None    => Promise.reject("Form data is invalid")
    case Some(c) => Promise.resolve(c -> dir)
end extractVote

enum Dir:
  case Up, Down

enum Color:
  case Red, Green, Blue

  override def toString() =
    this match
      case Red   => "red"
      case Blue  => "blue"
      case Green => "green"

object Color:
  import Color.*
  def fromString(s: String): Option[Color] =
    s.trim.toLowerCase match
      case "red"   => Option(Red)
      case "green" => Option(Green)
      case "blue"  => Option(Blue)

class ColorsDB(kv: KVNamespace, strength: Int):

  def change(col: Color, dir: Dir) =
    dir match
      case Dir.Up   => increment(col)
      case Dir.Down => decrement(col)

  def get(col: Color) =
    kv.get(key(col)).map {
      case s: String =>
        s.toIntOption.getOrElse(0)
      case null => 0
    }

  private def increment(col: Color) =
    for
      value <- get(col)
      next = (value + strength).min(255)
      _ <- kv.put(key(col), next.toString)
    yield ()

  private def decrement(col: Color) =
    for
      value <- get(col)
      next = (value - strength).max(0)
      _ <- kv.put(key(col), next.toString)
    yield ()

  private def key(col: Color) =
    "counter-" + col.toString
end ColorsDB
