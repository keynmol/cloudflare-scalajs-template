package app

import scala.annotation.implicitNotFound
import scala.scalajs.js.Promise
import scala.util.NotGiven

extension [A](p: Promise[A])
  inline def map[B](inline f: A => B)(using
      @implicitNotFound("Seems like you need `flatMap` instead of `map`")
      ev: NotGiven[B <:< Promise[?]]
  ): Promise[B] =
    p.`then`(f)

  inline def flatMap[B](inline f: A => Promise[B]): Promise[B] =
    p.`then`(f)

  inline def withFilter(inline pred: A => Boolean): Promise[A] =
    flatMap { a =>
      if pred(a) then Promise.resolve(a)
      else Promise.reject(MatchError(a))
    }

  inline def *>[B](other: Promise[B]): Promise[B] =
    flatMap(_ => other)
end extension
