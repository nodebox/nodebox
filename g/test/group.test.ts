import { expect, it } from "vitest";
import { Group, Rect, Circle, ShapeType } from "../src";

it("can create a group with shapes", () => {
  const rect = new Rect(10, 10, 100, 100);
  const circle = new Circle(50, 50, 50);
  const group = new Group();
  group.children.push(rect);
  group.children.push(circle);
  expect(group.children.length).toEqual(2);

  const clone = group.clone() as Group;
  expect(clone.type === ShapeType.Group);
  expect(clone.children.length).toEqual(2);
});

it("group attributes should have no fill and stroke", () => {
  const group = new Group();
  const attrs = group._getAttributes();
  expect(attrs).toEqual({});
});

// it("getIds should return correct ids", () => {
//   const group = new Group("main");
//   const rect1 = new Rect(0, 0, 10, 10);
//   rect1.id = "rect1 box red";
//   const rect2 = new Rect(0, 0, 10, 10);
//   rect2.id = "rect2 box blue";
//   const subGroup = new Group("sub green");
//   const circle = new Circle(0, 0, 5);
//   circle.id = "circle1 round blue";

//   group.add(rect1);
//   group.add(rect2);
//   group.add(subGroup);
//   subGroup.add(circle);

//   expect(group.getTags()).toEqual(["main", "rect1 box red", "rect2 box blue", "sub green", "circle1 round blue"]);
//   expect(group.getTags(["box"])).toEqual(["rect1 box red", "rect2 box blue"]);
//   // expect(group.getTags(["!blue"])).toEqual(["main", "rect1 box red", "sub green"]);
//   expect(group.getTags(["blue", "round"])).toEqual(["rect2 box blue", "circle1 round blue"]);
// });

// it("selectShapes should create filtered group", () => {
//   const group = new Group("main");
//   const rect1 = new Rect(0, 0, 10, 10);
//   rect1.id = "rect1 shape red";
//   const rect2 = new Rect(0, 0, 10, 10);
//   rect2.id = "rect2 shape blue";
//   const subGroup = new Group("sub");
//   const circle = new Circle(0, 0, 5);
//   circle.id = "circle1 shape blue";

//   subGroup.add(circle);
//   group.add(rect1);
//   group.add(rect2);
//   group.add(subGroup);

//   const filtered = group.selectShapes(["blue"]);
//   expect(filtered.children.length).toBe(2);
//   expect(filtered.children[0].id).toBe("rect2 shape blue");
//   expect(filtered.children[1].id).toBe("circle1 shape blue");

//   const multiFiltered = group.selectShapes(["shape", "red"]);
//   expect(multiFiltered.children.length).toBe(1);
//   expect(multiFiltered.children[0].id).toBe("rect1 shape red");
// });
