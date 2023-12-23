package utils

object WithOpt {
  extension [O, F](obj: O)
    def withOpt(optValue: Option[F], setField: (O, F) => O): O =
      optValue.map(setField(obj, _)).getOrElse(obj)
}
