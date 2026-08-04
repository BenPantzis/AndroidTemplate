package com.template.android.core.testing

// Shared fixture builders for unit tests.
//
// Add a builder function for each domain model in your app.
// Use named parameters with sensible defaults so each test only overrides
// the field it cares about:
//
//   fun fakeFoo(
//       id: String = "1",
//       name: String = "Foo",
//       isActive: Boolean = true,
//   ): Foo = Foo(id = id, name = name, isActive = isActive)
//
// Keep builders in sync with model changes — a compile error here is better
// than a silent stale fixture.
