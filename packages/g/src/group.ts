import { Paint } from "./paint";
import { Shape, ShapeType, SvgAttributes } from "./shape";
import { Transform } from "./transform";
export class Group extends Shape {
  children: Shape[];
  height?: number;
  width?: number;
  viewBox?: string;

  constructor() {
    super(ShapeType.Group);
    this.children = [];
  }

  [Symbol.iterator]() {
    let index = -1;
    const children = this.children;

    return {
      next: () => ({
        value: children[++index],
        done: index >= children.length,
      }),
    };
  }

  clone(): Group {
    const group = new Group();
    group.children = this.children.map((child) => child.clone());
    group._cloneAttributes(this);
    return group;
  }

  add(shape: Shape) {
    this.children.push(shape);
  }

  push(shape: Shape) {
    this.children.push(shape);
  }

  getBounds() {
    if (this.children.length === 0) {
      return { left: 0, right: 0, top: 0, bottom: 0, centerX: 0, centerY: 0 };
    }

    let left_ = Infinity;
    let right_ = -Infinity;
    let top_ = Infinity;
    let bottom_ = -Infinity;

    this.children.forEach((child) => {
      if (typeof child.getBounds === "function") {
        const bounds = child.getBounds();

        left_ = Math.min(left_, bounds.left);
        right_ = Math.max(right_, bounds.right);
        top_ = Math.min(top_, bounds.top);
        bottom_ = Math.max(bottom_, bounds.bottom);
      }
    });

    const topLeft = Transform.applyMatrixTransform(left_, top_, this.transform.matrix);
    const topRight = Transform.applyMatrixTransform(right_, top_, this.transform.matrix);
    const bottomLeft = Transform.applyMatrixTransform(left_, bottom_, this.transform.matrix);
    const bottomRight = Transform.applyMatrixTransform(right_, bottom_, this.transform.matrix);

    const left = Math.min(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const right = Math.max(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const top = Math.min(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
    const bottom = Math.max(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);

    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    return { left, right, top, bottom, centerX, centerY };
  }

  traverse(tagsIn: string[], callback: (context: Group | Shape) => void): void {
    if (tagsIn.length === 1 && tagsIn[0] == "") tagsIn = [];

    const tags = Array.isArray(tagsIn) ? tagsIn : [tagsIn];

    const posTagMatch = tags.filter((tag) => !tag.startsWith("!"));
    const negTagMatch = tags.filter((tag) => tag.startsWith("!")).map((tag) => tag.replace("!", ""));

    const matchesTags = (item: Shape): boolean => {
      const itemTags = item.tags || [];
      if (posTagMatch.length === 0 && negTagMatch.length === 0) {
        return true;
      }

      if (posTagMatch.length > 0 && !posTagMatch.some((tag) => itemTags.includes(tag))) {
        return false;
      }

      if (negTagMatch.some((tag) => itemTags.includes(tag))) {
        return false;
      }

      return true;
    };

    // Check current group
    if (matchesTags(this)) {
      callback(this);
    }

    // Process children
    this.children.forEach((child) => {
      if ("traverse" in child) {
        // Recursively process groups
        (child as Group).traverse(tags, callback);
      } else if (matchesTags(child)) {
        callback(child);
      }
    });
  }

  selectShapes(tagsIn: string[]) {
    if (tagsIn.length === 0 || (tagsIn.length === 1 && tagsIn[0] === "")) {
      return this.clone();
    }
    const posTagMatch = tagsIn.filter((tag) => !tag.startsWith("!"));
    const negTagMatch = tagsIn.filter((tag) => tag.startsWith("!")).map((tag) => tag.replace("!", ""));

    const matchesTags = (item: any): boolean => {
      const itemTags = item.tags || [];

      if (posTagMatch.length > 0 && !posTagMatch.some((tag) => itemTags.includes(tag))) {
        return false;
      }

      if (negTagMatch.some((tag) => itemTags.includes(tag))) {
        return false;
      }

      return true;
    };

    const filtered = this.clone();

    filtered.children = filtered.children
      .filter((child) => {
        if ("traverse" in child) {
          if (child.tags && negTagMatch.some((tag) => child.tags.includes(tag))) {
            return false;
          }
          if (posTagMatch.length > 0 && child.tags && posTagMatch.some((tag) => child.tags.includes(tag))) {
            return true;
          }
          return (child as Group).selectShapes(tagsIn).children.length > 0;
        } else {
          return matchesTags(child);
        }
      })
      .map((child) => {
        if ("traverse" in child) {
          if (posTagMatch.length > 0 && child.tags && posTagMatch.some((tag) => child.tags.includes(tag))) {
            return child.clone();
          }
          return (child as Group).selectShapes(tagsIn);
        }
        return child;
      });

    return filtered;
  }

  public _updateAttributes(updates: { [key: string]: any }) {
    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined) {
        (this as any)[key] = value;
      }
    });
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    this.children.forEach((child) => {
      child.bakeTransform(matrix);
    });
    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    let d = "";
    this.children.forEach((child) => {
      d += child.toPathData(applyTransform, matrix);
    });
    return d;
  }
}
