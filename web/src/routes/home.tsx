import React from "react";
import { Link } from "wouter";
import NodeBoxLogo from "../components/nodebox-logo";
import { useAuth } from "../auth-context";
import Slideshow from "../components/slideshow.tsx";

interface IconMapProps {
  [key: string]: string | string[];
}

// prettier-ignore
const ICON_MAP: IconMapProps = {
    "visual-interface": ["M20 3H4c-1.103 0-2 .897-2 2v14c0 1.103.897 2 2 2h16c1.103 0 2-.897 2-2V5c0-1.103-.897-2-2-2zm0 2 .001 4H4V5h16zM4 19v-8h16.001l.001 8H4z", "M14 6h2v2h-2zm3 0h2v2h-2z"],
    "access-the-code": "M12 21.985c-.275 0-.532-.074-.772-.202l-2.439-1.448c-.365-.203-.182-.277-.072-.314.496-.165.588-.201 1.101-.493.056-.037.129-.02.185.017l1.87 1.12c.074.036.166.036.221 0l7.319-4.237c.074-.036.11-.11.11-.202V7.768c0-.091-.036-.165-.11-.201l-7.319-4.219c-.073-.037-.165-.037-.221 0L4.552 7.566c-.073.036-.11.129-.11.201v8.457c0 .073.037.166.11.202l2 1.157c1.082.548 1.762-.095 1.762-.735V8.502c0-.11.091-.221.22-.221h.936c.108 0 .22.092.22.221v8.347c0 1.449-.788 2.294-2.164 2.294-.422 0-.752 0-1.688-.46l-1.925-1.099a1.55 1.55 0 0 1-.771-1.34V7.786c0-.55.293-1.064.771-1.339l7.316-4.237a1.637 1.637 0 0 1 1.544 0l7.317 4.237c.479.274.771.789.771 1.339v8.458c0 .549-.293 1.063-.771 1.34l-7.317 4.236c-.241.11-.516.165-.773.165zm2.256-5.816c-3.21 0-3.87-1.468-3.87-2.714 0-.11.092-.221.22-.221h.954c.11 0 .201.073.201.184.147.971.568 1.449 2.514 1.449 1.54 0 2.202-.35 2.202-1.175 0-.477-.185-.825-2.587-1.063-1.999-.2-3.246-.643-3.246-2.238 0-1.485 1.247-2.366 3.339-2.366 2.347 0 3.503.809 3.649 2.568a.297.297 0 0 1-.056.165c-.037.036-.091.073-.146.073h-.953a.212.212 0 0 1-.202-.164c-.221-1.012-.789-1.34-2.292-1.34-1.689 0-1.891.587-1.891 1.027 0 .531.237.696 2.514.99 2.256.293 3.32.715 3.32 2.294-.02 1.615-1.339 2.531-3.67 2.531z",
    "share-and-contribute": "M5.5 15a3.51 3.51 0 0 0 2.36-.93l6.26 3.58a3.06 3.06 0 0 0-.12.85 3.53 3.53 0 1 0 1.14-2.57l-6.26-3.58a2.74 2.74 0 0 0 .12-.76l6.15-3.52A3.49 3.49 0 1 0 14 5.5a3.35 3.35 0 0 0 .12.85L8.43 9.6A3.5 3.5 0 1 0 5.5 15zm12 2a1.5 1.5 0 1 1-1.5 1.5 1.5 1.5 0 0 1 1.5-1.5zm0-13A1.5 1.5 0 1 1 16 5.5 1.5 1.5 0 0 1 17.5 4zm-12 6A1.5 1.5 0 1 1 4 11.5 1.5 1.5 0 0 1 5.5 10z",
    "help-built-in": ["M12 6a3.939 3.939 0 0 0-3.934 3.934h2C10.066 8.867 10.934 8 12 8s1.934.867 1.934 1.934c0 .598-.481 1.032-1.216 1.626a9.208 9.208 0 0 0-.691.599c-.998.997-1.027 2.056-1.027 2.174V15h2l-.001-.633c.001-.016.033-.386.441-.793.15-.15.339-.3.535-.458.779-.631 1.958-1.584 1.958-3.182A3.937 3.937 0 0 0 12 6zm-1 10h2v2h-2z", "M12 2C6.486 2 2 6.486 2 12s4.486 10 10 10 10-4.486 10-10S17.514 2 12 2zm0 18c-4.411 0-8-3.589-8-8s3.589-8 8-8 8 3.589 8 8-3.589 8-8 8z"],

    "import": "M20 14V8l-6-6H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4h-7v3l-5-4 5-4v3h7zM13 4l5 5h-5V4z",
    "process": "M21 3H5a1 1 0 0 0-1 1v2.59c0 .523.213 1.037.583 1.407L10 13.414V21a1.001 1.001 0 0 0 1.447.895l4-2c.339-.17.553-.516.553-.895v-5.586l5.417-5.417c.37-.37.583-.884.583-1.407V4a1 1 0 0 0-1-1zm-6.707 9.293A.996.996 0 0 0 14 13v5.382l-2 1V13a.996.996 0 0 0-.293-.707L6 6.59V5h14.001l.002 1.583-5.71 5.71z",
    "visualize": ["M13 6c2.507.423 4.577 2.493 5 5h4c-.471-4.717-4.283-8.529-9-9v4z", "M18 13c-.478 2.833-2.982 4.949-5.949 4.949-3.309 0-6-2.691-6-6C6.051 8.982 8.167 6.478 11 6V2c-5.046.504-8.949 4.773-8.949 9.949 0 5.514 4.486 10 10 10 5.176 0 9.445-3.903 9.949-8.949h-4z"],
    "embed": "m7.375 16.781 1.25-1.562L4.601 12l4.024-3.219-1.25-1.562-5 4a1 1 0 0 0 0 1.562l5 4zm9.25-9.562-1.25 1.562L19.399 12l-4.024 3.219 1.25 1.562 5-4a1 1 0 0 0 0-1.562l-5-4zm-1.649-4.003-4 18-1.953-.434 4-18z"
}

interface FeatureProps {
  icon: string;
  title: string;
  description: string;
}

function Feature({ icon, title, description }: FeatureProps) {
  const d = ICON_MAP[icon];

  return (
    <div className="feature flex flex-wrap md:flex-nowrap items-center gap-4 my-8">
      <svg className="icon w-16" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="white">
        {Array.isArray(d) ? d.map((path, i) => <path key={i} d={path} />) : <path d={d} />}
      </svg>
      <div className="text flex flex-col">
        <h3 className="font-bold text-xl">{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

interface FigureProps {
  url: string;
  title: string;
  author: string;
}

function Figure({ url, title, author }: FigureProps) {
  return (
    <figure className="flex flex-col items-center gap-4">
      <img src={url} alt={title} className="rounded-lg" />
      <figcaption className="text-center flex gap-2">
        <h3 className="text-xs">{title}</h3>
        <p className="text-xs opacity-60">{author}</p>
      </figcaption>
    </figure>
  );
}

interface Line {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  speed: number;
}

export default function Home() {
  const { userId } = useAuth()!;

  React.useEffect(() => {
    function rand(min: number, max: number) {
      return min + Math.random() * (max - min);
    }

    function initHeaderBackground() {
      const gCanvas = document.getElementById("intro-canvas")! as HTMLCanvasElement;
      const gCtx = gCanvas.getContext("2d")!;
      const gScale = 10;
      const gConnectionDistanceSquared = 1000;
      let gLineCount: number;
      const gLines: Line[] = [];

      function distanceSquared(x1: number, y1: number, x2: number, y2: number) {
        const dx = x1 - x2;
        const dy = y1 - y2;
        return dx * dx + dy * dy;
      }

      function resizeCanvas() {
        const r = gCanvas.getBoundingClientRect();
        const windowWidth = r.width;
        const windowHeight = r.height;
        const ratio = window.devicePixelRatio || 1;
        gCanvas.width = windowWidth * ratio;
        gCanvas.height = windowHeight * ratio;

        gCanvas.style.width = windowWidth + "px";
        gCanvas.style.height = windowHeight + "px";

        gCtx.scale(ratio, ratio);

        return ratio;
      }

      function updateLines() {
        let delta;
        for (let i = 0; i < gLineCount; i++) {
          const line = gLines[i];
          line.y1 += line.speed;
          line.y2 += line.speed;
          if (line.speed > 0 && line.y1 > gCanvas.height) {
            delta = gCanvas.height;
            line.y1 -= delta;
            line.y2 -= delta;
          } else if (line.speed < 0 && line.y1 < 0) {
            delta = gCanvas.height;
            line.y1 += delta;
            line.y2 += delta;
          }
        }
      }

      function drawLines() {
        gCtx.lineWidth = 1;
        gCtx.strokeStyle = "rgba(255, 255, 255, 0.1)";
        gCtx.beginPath();
        for (let i = 0; i < gLineCount; i++) {
          const line = gLines[i];
          gCtx.moveTo(line.x1, line.y1);
          gCtx.lineTo(line.x2, line.y2);
        }
        gCtx.stroke();
      }

      function distanceLine(x1: number, y1: number, x2: number, y2: number) {
        const d = Math.abs(distanceSquared(x1, y1, x2, y2));
        if (d < gConnectionDistanceSquared) {
          gCtx.moveTo(x1, y1);
          gCtx.lineTo(x2, y2);
        }
      }

      function drawDistanceLines() {
        gCtx.lineWidth = 0.5;
        gCtx.strokeStyle = "rgba(175, 84, 87, 0.5)";
        gCtx.beginPath();
        for (let i = 0; i < gLineCount; i++) {
          const line1 = gLines[i];
          for (let j = i + 1; j < gLineCount; j++) {
            const line2 = gLines[j];
            distanceLine(line1.x1, line1.y1, line2.x1, line2.y1);
            distanceLine(line1.x1, line1.y1, line2.x2, line2.y2);
            distanceLine(line1.x2, line1.y2, line2.x1, line2.y1);
            distanceLine(line1.x2, line1.y2, line2.x2, line2.y2);
          }
        }
        gCtx.stroke();
      }

      function draw() {
        gCtx.clearRect(0, 0, gCanvas.width, gCanvas.height);
        updateLines();
        drawLines();
        drawDistanceLines();
        window.requestAnimationFrame(draw);
      }

      function makeLine() {
        const x1 = Math.floor(rand(4, gCanvas.width / gScale - 4)) * gScale;
        const y1 = Math.floor(rand(4, gCanvas.height / gScale - 4)) * gScale;
        const x2 = x1 + rand(-10, 10) * gScale;
        const y2 = y1 + rand(-10, 10) * gScale;
        return {
          x1: x1,
          y1: y1,
          x2: x2,
          y2: y2,
          speed: rand(-0.5, 0.5),
        };
      }

      function init() {
        const r = gCanvas.getBoundingClientRect();
        gCanvas.width = r.width;
        gCanvas.height = r.height;
        const ratio = resizeCanvas();
        gLineCount = Math.floor((r.width / 10) * ratio);
        for (let i = 0; i < gLineCount; i++) {
          gLines.push(makeLine());
        }
        draw();
        window.addEventListener("resize", resizeCanvas);
      }

      init();
    }

    function initHeaderHider() {
      let didScroll = false;
      let prevScrollY = 0;
      const SCROLL_DELTA = 5;

      document.addEventListener("scroll", function () {
        didScroll = true;
      });

      setInterval(function () {
        if (didScroll) {
          onScroll();
          didScroll = false;
        }
      }, 100);

      function onScroll() {
        if (Math.abs(window.scrollY - prevScrollY) <= SCROLL_DELTA) return;
        if (window.scrollY > prevScrollY) {
          // Scrolling down
          document.querySelector("header")!.classList.add("hidden");
        } else {
          // Scrolling up
          document.querySelector("header")!.classList.remove("hidden");
        }
        prevScrollY = window.scrollY;
      }
    }

    initHeaderBackground();
    initHeaderHider();
  }, []);

  return (
    <div id="home">
      <header className="flex items-center justify-between h-12 px-4 fixed top-0 w-full bg-black z-30">
        <a href="/" className="logo flex gap-2 items-center" target="_self">
          <NodeBoxLogo /> <span className="text-xs font-bold">NodeBox Live</span>
        </a>
        <nav className="text-xs flex gap-3">
          {!userId && <Link href="/auth/signup">Sign Up</Link>}
          {!userId && <Link href="/auth/login">Login</Link>}
          {userId && <Link href={`/${userId}`}>Projects</Link>}
          <a href="/guide/welcome">Guide</a>
          <a href="/gallery">Gallery</a>
        </nav>
      </header>

      <section className="hero overflow-hidden pt-12" style={{ height: "95vh" }}>
        <figure className="relative h-full">
          <img src="/images/home/home-hero-blur.png" alt="" className="h-full object-cover opacity-70" />
          <figcaption className="absolute bottom-1 right-1 text-xs flex gap-1 opacity-60">
            <span className="font-bold">Sun Flowers</span>
            <span className="author">Cvijeta Miljak</span>
          </figcaption>
        </figure>
        <div className="container absolute bottom-1/2">
          <h1 className="drop-shadow-xl text-4xl pl-4 max-w-2xl font-bold">
            Data Visualization and
            <br />
            Generative Design made easy.
          </h1>
        </div>
      </section>

      <section className="intro overflow-hidden relative flex justify-center items-center" style={{ height: "75vh" }}>
        <canvas className="w-full h-full absolute top-0 left-0" id="intro-canvas"></canvas>
        <article className="max-w-2xl">
          <h2 className="text-4xl font-bold mb-4">Presenting NodeBox Live</h2>
          <p>
            NodeBox Live is a web application for data visualization and generative design. It integrates seamlessly
            with the web, transforming data from multiple services into interactive visualizations.
          </p>
        </article>
      </section>

      <section className="gui bg-zinc-800 py-16 shadow-xl">
        <article className="max-w-4xl m-auto">
          <h2 className="text-center text-4xl mb-8 font-bold">Node-based Visual Workflow</h2>
          <Slideshow>
            <img src="/images/home/screenshot-1.png" />
            <img src="/images/home/screenshot-2.png" />
            <img src="/images/home/screenshot-3.png" />
            <img src="/images/home/screenshot-4.png" />
          </Slideshow>
          <p className="text-xs text-center mt-2">
            No coding skills? No problem. Connect nodes together instead of programming with code.
          </p>
          <div className="features md:max-w-lg m-auto pt-8">
            <Feature
              icon="visual-interface"
              title="Visual Interface"
              description="Our node-based workflow shows what's going on at every stage of the project."
            />
            <Feature
              icon="access-the-code"
              title="Access the Code"
              description="Write your own JavaScript functions from scratch — or customize ours."
            />
            <Feature
              icon="share-and-contribute"
              title="Share and Contribute"
              description="Share projects with other people and re-use and remix each other's work."
            />
            <Feature
              icon="help-built-in"
              title="Help Built-in"
              description="Contextual help is built right in along with examples and a gallery with examples."
            />
          </div>
        </article>
      </section>

      <section className="viz py-16">
        <article className="max-w-4xl m-auto flex flex-col items-center gap-4">
          <h2 className="text-4xl font-bold mb-4">Unique Visualizations that Run on the Web</h2>
          <p className="prompt max-w-md text-center">
            NodeBox Live runs natively on the web. Visualizations made with NodeBox Live integrate seamlessly in other
            websites.
          </p>
          <Figure
            url="/images/home/nbl-wind-type.jpg"
            title="Wind Type"
            author="Milda Siulyte &amp; Vincentas Kuodis"
          />

          <div className="features md:max-w-lg m-auto my-32">
            <Feature
              icon="import"
              title="Import"
              description="Work with CSV and JSON data or talk to an external web API."
            />
            <Feature
              icon="process"
              title="Process"
              description="Filter and aggregate your data using our practical data processing nodes."
            />
            <Feature
              icon="visualize"
              title="Visualize"
              description="Present your data using a collection of built-in shapes — or make your own."
            />
            <Feature
              icon="embed"
              title="Embed"
              description="Embed your visualizations in other websites and applications."
            />
          </div>
        </article>
      </section>

      <section className="gallery py-16 bg-zinc-800 shadow-xl">
        <article className="max-w-4xl m-auto">
          <h2 className="text-center text-4xl mb-8 font-bold">Selected Works</h2>
          <div className="images flex flex-col gap-8">
            <Figure
              url="/images/home/nb3-film-speech-thumb.jpg"
              title="50 Years of Film Speech"
              author="Jonas Lekevicius, Juste Ziliute, Augustinas Paukste"
            />
            <Figure
              url="/images/home/nb3-eurovoices-thumb.jpg"
              title="Eurovoices"
              author="Dalia Kemeklytė &amp; Viktorija Pampuščenko"
            />
            <Figure url="/images/home/nbl-goldberg-thumb.jpg" title="Goldberg Variations" author="Anton Sovetov" />
            <Figure url="/images/home/nbl-surnames-thumb.jpg" title="Finnish Surnames" author="Eemeli Nieminen" />
          </div>
        </article>
      </section>

      <section className="about py-16">
        <article className="max-w-4xl m-auto flex flex-col items-center">
          <h2 className="text-center text-4xl mb-4 font-bold">Designed in Antwerp</h2>
          <div className="sla py-8">
            <svg className="h-16" viewBox="0 0 210 36" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path fillRule="evenodd" clipRule="evenodd" d="M2 36H36V32H2V36ZM0 30H4V0H0V30Z" fill="white" />
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M15.334 12.968H18.322C18.242 10.862 16.302 10.302 13.362 10.302C10.398 10.302 8.486 10.872 8.486 12.882C8.486 14.482 9.562 15.069 11.386 15.248L14.504 15.548C15.115 15.605 15.55 15.768 15.55 16.262C15.55 16.808 14.97 17.064 13.54 17.064C12.227 17.064 11.459 16.86 11.277 15.886H8.282C8.375 18.267 10.21 18.805 13.408 18.805C16.751 18.805 18.61 18.303 18.61 16.079C18.61 14.466 17.595 13.775 15.743 13.619L12.518 13.34C11.776 13.274 11.451 13.13 11.451 12.735C11.451 12.285 11.91 12 13.251 12C14.497 12 15.198 12.25 15.334 12.968ZM19.816 18.543H22.406V10.563H19.816V18.543ZM31.463 10.563V15.523H31.393L26.976 10.564H23.924V18.545H26.394V13.506H26.464L30.949 18.544H33.933V10.563H31.463ZM44.656 10.563H34.85V12.61H38.457V18.544H41.048V12.61H44.656V10.563ZM58.617 18.544V16.524H52.401V10.563H49.811V18.544H58.617ZM66.859 10.563V15.083C66.859 16.379 65.905 16.776 64.473 16.776C63.04 16.776 62.089 16.379 62.089 15.084V10.563H59.499V15.127C59.499 17.989 61.446 18.805 64.473 18.805C67.5 18.805 69.449 17.989 69.449 15.127V10.563H66.859ZM78.404 13.677H81.497C81.43 11.542 79.569 10.302 76.117 10.302C72.641 10.302 70.595 11.644 70.595 14.553C70.595 17.463 72.641 18.805 76.116 18.805C79.569 18.805 81.463 17.565 81.52 15.262H78.395C78.315 16.081 77.63 16.795 76.113 16.795C74.325 16.795 73.443 15.972 73.443 14.553C73.443 13.135 74.325 12.311 76.113 12.311C77.628 12.311 78.326 13.07 78.404 13.677ZM90.274 18.544H93.141L89.075 10.563H85.67L81.57 18.544H84.438L85.055 17.285H89.657L90.276 18.544H90.274ZM85.957 15.442L87.316 12.666H87.384L88.749 15.442H85.957ZM100.307 12.968H103.296C103.216 10.862 101.275 10.302 98.335 10.302C95.371 10.302 93.459 10.872 93.459 12.882C93.459 14.482 94.535 15.069 96.359 15.248L99.477 15.548C100.089 15.605 100.524 15.768 100.524 16.262C100.524 16.808 99.944 17.064 98.514 17.064C97.2 17.064 96.432 16.86 96.249 15.886H93.256C93.348 18.267 95.182 18.805 98.381 18.805C101.724 18.805 103.583 18.303 103.583 16.079C103.583 14.466 102.568 13.775 100.716 13.619L97.491 13.341C96.749 13.275 96.424 13.131 96.424 12.736C96.424 12.286 96.884 12.001 98.224 12.001C99.47 12.001 100.172 12.25 100.307 12.968ZM116.864 18.544H119.729L115.664 10.563H112.258L108.159 18.544H111.025L111.642 17.285H116.245L116.864 18.544ZM112.545 15.442L113.904 12.666H113.974L115.338 15.442H112.545ZM128.051 10.562V15.524H127.981L123.561 10.563H120.509V18.544H122.979V13.506H123.049L127.534 18.544H130.519V10.563H128.049L128.051 10.562ZM141.243 10.562H131.437V12.61H135.045V18.544H137.635V12.61H141.243V10.562ZM153.583 18.544L156.111 10.563H153.29L151.905 15.691H151.835L150.408 10.563H147.56L146.143 15.691H146.073L144.688 10.563H141.845L144.336 18.544H147.541L148.948 13.493H149.018L150.426 18.544H153.583ZM159.488 12.39H165.673V10.563H156.897V18.544H165.796V16.708H159.488V15.307H165.418V13.617H159.488V12.39ZM175.505 15.533V15.443C176.518 15.206 177.255 14.644 177.255 13.191C177.255 11.452 176.377 10.563 174.072 10.563H167.167V18.544H169.757V15.982H172.897L174.457 18.544H177.459L175.505 15.533ZM173.448 14.168H169.758V12.55H173.448C174.127 12.55 174.401 12.814 174.401 13.353C174.401 13.903 174.127 14.167 173.448 14.167V14.168ZM185.143 10.563H178.508V18.544H181.098V15.947H185.143C187.457 15.947 188.346 15.069 188.346 13.257C188.346 11.452 187.466 10.563 185.143 10.563ZM184.485 14.184H181.099V12.46H184.485C185.2 12.46 185.499 12.737 185.499 13.321C185.499 13.897 185.199 14.184 184.485 14.184ZM191.948 12.39H198.133V10.563H189.357V18.544H198.255V16.708H191.948V15.307H197.877V13.617H191.947L191.948 12.39ZM207.167 10.563V15.523H207.097L202.678 10.563H199.626V18.544H202.096V13.506H202.166L206.651 18.544H209.636V10.563H207.166H207.167Z"
                fill="white"
              />
            </svg>
          </div>
          <div className="flex flex-col gap-4 mt-4">
            <p className="text-lg">
              NodeBox Live is designed and coded at{" "}
              <a href="https://www.sintlucasantwerpen.be/" className="opacity-60">
                Sint Lucas Antwerpen
              </a>
              , a cutting-edge art school with a digital research group focused on usability, computational creativity
              and artificial intelligence.
            </p>
            <p className="text-lg">
              From 2023 to 2024, NodeBox Live is being developed as part of the "Story Graphics" research project in
              collaboration with{" "}
              <a href="https://www.deeluitgeverij.be/" className="opacity-60">
                De Deeluitgeverij
              </a>
              ,{" "}
              <a href="https://www.eoswetenschap.eu/" className="opacity-60">
                EOS Wetenschap
              </a>{" "}
              and{" "}
              <a href="https://treecompany.be/" className="opacity-60">
                Tree Company
              </a>
              .
            </p>
          </div>
        </article>
      </section>

      <section className="signup py-16 bg-zinc-800 shadow-xl" id="signup">
        <article className="max-w-4xl m-auto flex flex-col items-center ">
          <h2 className="text-center text-4xl mb-8 font-bold">Sign Up</h2>
          <p>Create a new account to get started with NodeBox Live.</p>
          <a className="cta px-8 py-3 bg-blue-500 rounded-lg font-bold mt-8" href="/auth/signup">
            Sign Up
          </a>
        </article>
      </section>

      <footer className="bg-black">
        <div className="max-w-lg m-auto flex flex-col items-center gap-2  py-8">
          <p className="text-xs">&copy; 2024 NodeBox Live</p>
          <p className="text-xs flex flex-wrap gap-2">
            <a href="/">Home</a>
            <a href="/guide/welcome">Guide</a>
            <a href="/gallery">Gallery</a>
          </p>
          <p className="text-xs flex flex-wrap gap-2">
            <a href="/terms">Terms</a>
            <a href="/privacy">Privacy</a>
          </p>
        </div>
      </footer>
    </div>
  );
}
