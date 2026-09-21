import { it, expect } from "vitest";
import {
  validateUsername,
  validateEmail,
  validatePassword,
  validateVersion,
  validateProjectId,
} from "../src/validate.js";

it("can validate user names", () => {
  expect(() => validateUsername("john")).not.toThrow();
  expect(() => validateUsername("john-smith")).not.toThrow();
  expect(() => validateUsername("john--smith")).not.toThrow();
  expect(() => validateUsername("john-smith-38")).not.toThrow();
  expect(() => validateUsername("john smith")).toThrow();
  expect(() => validateUsername("-")).toThrow();
  expect(() => validateUsername("")).toThrow();
  expect(() => validateUsername("a-super-long-user-name-that-goes-on-forever")).toThrow();
  expect(() => validateUsername("goto")).toThrow();
  expect(() => validateUsername("admin")).toThrow();
});

it("can validate e-mail addresses", () => {
  expect(() => validateEmail("test@example.com")).not.toThrow();
  expect(() => validateEmail("a.b.c@d.ee")).not.toThrow();
  expect(() => validateEmail("ZZZ")).toThrow();
  expect(() => validateEmail("test@@example.com")).toThrow();
  expect(() => validateEmail("test@example")).toThrow();
});

it("can validate passwords", () => {
  expect(() => validatePassword("password1")).not.toThrow();
  expect(() => validatePassword("")).toThrow();
  expect(() => validatePassword("1234")).toThrow();
});

it("can validate versions", () => {
  expect(() => validateVersion("1.0.0")).not.toThrow();
  expect(() => validateVersion("2.12.123")).not.toThrow();
  expect(() => validateVersion("")).toThrow();
  expect(() => validateVersion("1")).toThrow();
  expect(() => validateVersion("1.0")).toThrow();
  expect(() => validateVersion("1.a.b")).toThrow();
  expect(() => validateVersion("1.0.0-pre1")).toThrow();
});

it("can validate project names", () => {
  expect(() => validateProjectId("foo")).not.toThrow();
  expect(() => validateProjectId("foo2")).not.toThrow();
  expect(() => validateProjectId("camelCaseName")).not.toThrow();
  expect(() => validateProjectId("")).toThrow();
  expect(() => validateProjectId("x")).toThrow();
  expect(() => validateProjectId("anamethatistoolong")).toThrow();
  expect(() => validateProjectId("UPPERCASE")).toThrow();
  expect(() => validateProjectId("2foo")).toThrow();
  expect(() => validateProjectId("foo-with-dashes")).toThrow();
  expect(() => validateProjectId("goto")).toThrow();
  expect(() => validateProjectId("ndbx")).toThrow();
});
