import StaticPage from "../components/static-page";
import { useParams } from "wouter";
import { apiRoot } from "../config";
import Icon from "../components/icon";
import apiFetcher from "../lib/api-fetcher";
import useSWR from "swr";
import InlineMessage from "../components/inline-message";
import Markdown from "react-markdown";

interface GalleryItem {
  projectId: string;
  itemId: string;
  name: string;
  description: string;
  category: string;
  subKeyword: string;
}

function toTitleCase(str: string) {
  return str.replace(/\w\S*/g, (text) => text.charAt(0).toUpperCase() + text.substring(1).toLowerCase());
}

function groupBySubKeyword(items: GalleryItem[]) {
  return items.reduce<Record<string, GalleryItem[]>>((acc, item) => {
    acc[item.subKeyword] = acc[item.subKeyword] || [];
    acc[item.subKeyword].push(item);
    return acc;
  }, {});
}

function CategoryLink({
  label,
  img,
  href,
  description,
}: {
  label: string;
  img: string;
  href: string;
  description: string;
}) {
  return (
    <a className="bg-zinc-800 p-2 flex flex-row items-start rounded shadow gap-4" href={href}>
      <div
        className="w-48 h-24 bg-cover bg-center rounded bg-zinc-500"
        style={{ backgroundImage: `url(${img})` }}
        title={label}
      ></div>
      <Markdown className="flex-1 text-xs w-64 text-zinc-200">{description}</Markdown>
    </a>
  );
}

export default function GalleryCategory() {
  const { category } = useParams();
  const { data, error, isLoading } = useSWR(`${apiRoot}/api/gallery/${category}`, apiFetcher);

  const title = category?.replace(/-/g, " ");

  if (error) {
    return (
      <main id="gallery" className="px-2 md:px-8 pt-4">
        <h1 className="text-xl">Gallery</h1>
        <InlineMessage key={Date.now()}>The gallery does not exist (yet). Please try again later.</InlineMessage>
      </main>
    );
  }

  const groupedItems = data ? groupBySubKeyword(data.items) : {};

  return (
    <StaticPage title="NodeBox Gallery">
      <article className="max-w-5xl m-auto px-4 flex flex-col gap-4 mb-16">
        <h1 className="text-xl text-center flex items-center gap-2">
          <a className="border-b border-zinc-500" href="/gallery">
            NodeBox Gallery
          </a>
          <Icon name="chevron-right" size={24} />
          {toTitleCase(title || "")}
        </h1>
        {isLoading && <div className="border border-slate-200 rounded p-2 text-red-500">Loading...</div>}
        <h2 className="text-xl text-center"></h2>
        <Markdown>{data?.description}</Markdown>
        {Object.keys(groupedItems).map((subKeyword) => (
          <div key={subKeyword}>
            <h3 className="text-lg text-zinc-400 border-b border-zinc-600 mb-4 pb-2">{toTitleCase(subKeyword)}</h3>
            <div className="flex justify-start">
              <div className="flex flex-wrap gap-2">
                {groupedItems[subKeyword].map((item: GalleryItem) => (
                  <CategoryLink
                    key={item.itemId}
                    label={item.name}
                    img={`/api/svg/example/${item.projectId}/${item.itemId}`}
                    href={`/example/${item.projectId}#${item.itemId}`}
                    description={item.description}
                  />
                ))}
              </div>
            </div>
          </div>
        ))}
      </article>
    </StaticPage>
  );
}
