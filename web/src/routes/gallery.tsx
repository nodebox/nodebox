// import React from "react";
import StaticPage from "../components/static-page";

function CategoryLink({ label }: { label: string }) {
  const category = label.toLowerCase();
  const categoryLink = category.replace(/ /g, "-");
  const img = `/api/svg/example/${categoryLink}/0_1`;
  //   img = img.replace(/ /g, "-").toLowerCase();
  return (
    <a className="flex flex-col items-center gap-1" href={`/gallery/${categoryLink}`}>
      <div
        className="w-64 h-32 rounded shadow-lg bg-zinc-500"
        style={{ backgroundImage: `url(${img})`, backgroundSize: "cover", backgroundPosition: "center" }}
      />
      <span className="text-xs text-zinc-400">{label}</span>
    </a>
  );
}

export default function Gallery() {
  return (
    <StaticPage title="NodeBox Gallery">
      <article className="max-w-5xl m-auto px-4 flex flex-col gap-4 mb-16">
        <h1 className="text-xl text-center flex items-center gap-2">
          <span>NodeBox Gallery</span>
        </h1>
        <p className="text-sm text-zinc-500 mb-8">
          This is a collection of charts made with NodeBox, organized by themes.
        </p>
        <h2 className="text-2xl border-b border-zinc-700 pb-4">Relationship</h2>
        <p className="text-xs text-zinc-400 mb-2">
          Visualizations that explore the relationship between two or more variables.
        </p>
        <div className="flex justify-start">
          <div className="flex flex-wrap gap-4">
            <CategoryLink label="Scatter" />
            <CategoryLink label="Connected Scatter" />
            <CategoryLink label="Line" />
          </div>
        </div>

        <h2 className="text-2xl border-b border-zinc-700 pb-4">Comparison</h2>
        <p className="text-xs text-zinc-400 mb-2">Visualizations used to compare data points or groups directly.</p>
        <div className="flex justify-start">
          <div className="flex flex-wrap gap-4">
            <CategoryLink label="Bar" />
            <CategoryLink label="Bubble" />
            <CategoryLink label="Lollipop" />
          </div>
        </div>

        <h2 className="text-2xl border-b border-zinc-700 pb-4">Distribution</h2>
        <p className="text-xs text-zinc-400 mb-2">
          Visualizations that display how data points or values are spread across categories or ranges.
        </p>
        <div className="flex justify-start">
          <div className="flex flex-wrap gap-4">
            <CategoryLink label="Heatmap" />
            <CategoryLink label="Waffle" />
          </div>
        </div>

        <h2 className="text-2xl border-b border-zinc-700 pb-4">Ranking</h2>
        <p className="text-xs text-zinc-400 mb-2">
          Visualizations used to display the order or position of items based on a specific metric.
        </p>
        <div className="flex justify-start">
          <div className="flex flex-wrap gap-4">
            <CategoryLink label="Area" />
          </div>
        </div>

        {/* <h2 className="text-2xl">Gourmet</h2>
        <div className="flex justify-center">
          <div className="flex flex-wrap lg:gap-20 md:gap-12 gap-4  ">
            <CategoryLink label="Waffle" />
          </div>
        </div> */}
      </article>
    </StaticPage>
  );
}
