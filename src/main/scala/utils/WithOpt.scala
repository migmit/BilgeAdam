package utils

object WithOpt {
  extension [O, F](obj: O)
    /** Utility function, optionally setting one field
      *
      * @param optValue
      *   value to set, or `None` if setting not required
      * @param setField
      *   method to set the fielld
      * @return
      *   same object, if `optValue` is `None`, or the updated object otherwise
      */
    def withOpt(optValue: Option[F], setField: (O, F) => O): O =
      optValue.map(setField(obj, _)).getOrElse(obj)
}
