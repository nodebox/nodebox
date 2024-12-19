import StaticPage from "../components/static-page";

export default function Privacy() {
  return (
    <StaticPage title="NodeBox Live Privacy Policy">
      <article className="max-w-4xl m-auto flex flex-col gap-4 py-16">
        <p>
          If you choose to browse and use this website, you are agreeing to comply with and be bound by the following
          privacy policy, which together with the terms and conditions governs KdG's relationship with you in relation
          to this website. If you disagree with any part of these terms and conditions, please do not use our website.
        </p>

        <h2 className="text-xl font-bold">Information we collect</h2>

        <p>
          We collect information to understand your needs and provide you with a better service. The information we
          collect includes basic information like your name and e-mail address, and other information like what pages
          you visit.
        </p>

        <p>
          We collect this information through the information you give us when you sign up (name, email and if you are
          using paid services, payment information) and we also collect information we get from your use of our service.
        </p>

        <p>
          This information includes device information (what device you use our services with), location information
          (geographical location based on your IP address and language settings), log information (automatically
          collected information on how you use the service, such as the pages you visit, search queries you perform,
          ...)
        </p>

        <h2 className="text-xl font-bold">Cookies</h2>

        <p>
          A cookie is a small file which asks permission to be placed on your computer's hard drive. Once you agree, the
          file is added and the cookie helps analyse web traffic or lets you know when you visit a particular site.
          Cookies allow web applications to respond to you as an individual. The web application can tailor its
          operations to your needs, likes and dislikes by gathering and remembering information about your preferences.
        </p>

        <p>
          We use traffic log cookies to identify which pages are being used. This helps us analyse data about webpage
          traffic and improve our website in order to tailor it to customer needs. We only use this information for
          statistical analysis purposes and then the data is removed from the system.
        </p>

        <p>
          Overall, cookies help us provide you with a better website by enabling us to monitor which pages you find
          useful and which you do not. A cookie in no way gives us access to your computer or any information about you,
          other than the data you choose to share with us.
        </p>

        <p>
          You can choose to accept or decline cookies. Most web browsers automatically accept cookies, but you can
          usually modify your browser setting to decline cookies if you prefer. This may prevent you from taking full
          advantage of the website.
        </p>

        <h2 className="text-xl font-bold">How we use information we collect</h2>

        <p>
          We require this information to understand your needs and provide you with a better service. The information is
          not shared with or sold to other organizations for commercial purposes, except when sharing information is
          necessary to prevent or investigate illegal activities such as but not limited to suspected fraud, threats to
          the safety of a person, violations of terms and policies on this website or in any other case required by law.
        </p>

        <p>
          If NodeBox Live is acquired by or merged with another company, we will transfer your information to that
          company. We will however first notify you before transferring the information, if this implies a different
          privacy policy will apply.
        </p>

        <h2 className="text-xl font-bold">Data Storage</h2>

        <p>
          KdG uses third-party vendors and hosting partners to provide the necessary hardware, software, networking,
          storage, and related technology required to run the Service. KdG owns the code, databases, and all rights to
          the NodeBox Live application, but you retain all rights to your own data.
        </p>

        <h2 className="text-xl font-bold">Controlling your personal information</h2>

        <p>
          We will not sell, distribute or lease your personal information to third parties unless we have your
          permission or are required by law to do so.
        </p>

        <p>
          You may request details of personal information which we hold about you under the Belgian Privacy regulations.
          If you would like a copy of the information held on you please write to{" "}
          <em>Karel de Grote Hogeschool, EMRG, Brusselstraat 45, 2018 Antwerp, Belgium</em>.
        </p>

        <p>
          If you believe that any information we are holding on you is incorrect or incomplete, please write to or email
          us as soon as possible at the above address. We will promptly correct any information found to be incorrect.
        </p>

        <h2 className="text-xl font-bold">Changes</h2>

        <p>
          KdG may change this policy from time to time to reflect changes in the law or the Services. Changes will be
          made by updating this page. You should check this page from time to time to ensure that you are happy with any
          changes. This policy is effective from Dec 1, 2014. If you do not agree to any changes in these terms, you
          should stop using the Services.
        </p>

        <h2 className="text-xl font-bold">Questions</h2>

        <p>
          If you have any questions about this Privacy Policy, please use our{" "}
          <a href="/contact" className="underline">
            contact form
          </a>
          .
        </p>
      </article>
    </StaticPage>
  );
}
