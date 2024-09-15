"use client";

import { useState, useEffect } from 'react';

interface SentimentData {
  AverageNegative: number;
  AverageNeutral: number;
  AveragePositive: number;
  CountNeutral: number;
  CountPositive: number;
  CountNegative: number;
}

interface SentimentValues {
  [key: string]: SentimentData;
}

// Data for subtitles about each show
const showDetails: Record<string, { releaseDate: string; summary: string }> = {
  "The Acolyte": {
    releaseDate: "2024",
    summary: "A mystery-thriller series set in the final days of the High Republic era.",
  },
  "The Mandalorian": {
    releaseDate: "2019 - 2023",
    summary: "The story of a lone bounty hunter in the outer reaches of the galaxy.",
  },
  "Andor": {
    releaseDate: "2022 -",
    summary: "A prequel to Rogue One focusing on Cassian Andor's early days in the Rebellion.",
  },
};

export default function GalaxyOMeter() {
  const [sentimentData, setSentimentData] = useState<SentimentValues | null>(null);
  const [showNeutral, setShowNeutral] = useState(true); // State for toggle
  const [tooltip, setTooltip] = useState<{ text: string; x: number; y: number } | null>(null); // State for tooltip

  useEffect(() => {
    // Fetch data from the new JSON file location in the public folder
    fetch('/galaxy-o-meter/averageSentiment.json')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then((data) => setSentimentData(data))
      .catch((error) => console.error('Error fetching sentiment data:', error));
  }, []);

  if (!sentimentData) {
    return <p>Loading sentiment data...</p>;
  }

  // Function to handle tooltip display
  const showTooltip = (e: React.MouseEvent, text: string) => {
    setTooltip({ text, x: e.clientX, y: e.clientY });
  };

  const hideTooltip = () => {
    setTooltip(null);
  };

  // Function to render sentiment data with stacked bar chart and subtitle
  const renderSentiment = (show: string, data: SentimentData) => {
    // Calculate widths for the bar sections
    const positiveWidth = data.AveragePositive * 100;
    const neutralWidth = data.AverageNeutral * 100;
    const negativeWidth = data.AverageNegative * 100;

    const totalWidth = showNeutral ? positiveWidth + neutralWidth + negativeWidth : positiveWidth + negativeWidth;

    // Construct Twitter search URL
    const twitterSearchUrl = `https://twitter.com/search?q=${encodeURIComponent(show)}&src=typed_query`;

    return (
      <div className="mb-8 relative" key={show}>
        {/* Show Title with Link to Twitter Search */}
        <h2 className="font-semibold text-xl mb-1">
          <a href={twitterSearchUrl} target="_blank" rel="noopener noreferrer" className="hover:underline">
            {show}
          </a>
        </h2>
        {/* Subtitle with release date and summary */}
        <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-4">
          {showDetails[show].releaseDate} • {showDetails[show].summary}
        </p>

        {/* Stacked Bar Chart */}
        <div className="relative w-full h-6 bg-neutral-200 rounded-full mb-2 overflow-hidden">
          {/* Positive Bar */}
          <div
            className="absolute top-0 left-0 h-full bg-green-500 rounded-l-full cursor-pointer"
            style={{ width: `${(positiveWidth / totalWidth) * 100}%` }}
            onMouseEnter={(e) => showTooltip(e, `Positive count: ${data.CountPositive}`)}
            onMouseLeave={hideTooltip}
          ></div>
          {/* Neutral Bar (conditionally rendered) */}
          {showNeutral && (
            <div
              className="absolute top-0 h-full bg-yellow-500 cursor-pointer"
              style={{ left: `${(positiveWidth / totalWidth) * 100}%`, width: `${(neutralWidth / totalWidth) * 100}%` }}
              onMouseEnter={(e) => showTooltip(e, `Neutral count: ${data.CountNeutral}`)}
              onMouseLeave={hideTooltip}
            ></div>
          )}
          {/* Negative Bar */}
          <div
            className={`absolute top-0 h-full bg-red-500 cursor-pointer ${showNeutral ? '' : 'rounded-r-full'}`}
            style={{
              left: `${(showNeutral ? (positiveWidth + neutralWidth) / totalWidth : positiveWidth / totalWidth) * 100}%`,
              width: `${(negativeWidth / totalWidth) * 100}%`,
            }}
            onMouseEnter={(e) => showTooltip(e, `Negative count: ${data.CountNegative}`)}
            onMouseLeave={hideTooltip}
          ></div>
        </div>
        <div className="mt-2 text-xs text-neutral-500">
          <p>
            {data.CountPositive + data.CountNeutral + data.CountNegative} tweets analyzed as of 07/10/2024
          </p>
        </div>
      </div>
    );
  };

  return (
    <section>
      <h1 className="font-semibold text-2xl mb-8 tracking-tighter">Galaxy-O-Meter: How do the Fans Feel?</h1>
      <p className="mb-2">Using Azure Cognitive Services, I ran a sentiment analysis on tweets referencing three recent Star Wars shows.</p>
      <p className="mb-6">
        View source code <a href="https://github.com/roshda/galaxy-o-meter" className="text-blue-500 hover:underline">here</a>.
      </p>

      {/* Toggle for showing/hiding neutral sentiment */}
      <div className="flex items-center mb-6">
        <span className="mr-2 text-sm text-neutral-600 dark:text-neutral-400">Show Neutral Sentiment</span>
        <label className="relative inline-block w-10 h-6 align-middle select-none">
          <input
            type="checkbox"
            checked={showNeutral}
            onChange={() => setShowNeutral(!showNeutral)}
            className="toggle-checkbox absolute opacity-0 w-0 h-0"
          />
          <span
            className={`toggle-label block w-10 h-6 rounded-full cursor-pointer transition-colors duration-300 ease-in-out ${
              showNeutral ? 'bg-blue-500' : 'bg-gray-300'
            }`}
          >
            <span
              className={`absolute left-1 top-1 w-4 h-4 bg-white rounded-full transition-transform duration-300 ease-in-out ${
                showNeutral ? 'translate-x-4' : ''
              }`}
            ></span>
          </span>
        </label>
      </div>

      {Object.entries(sentimentData).map(([show, data]) => renderSentiment(show, data))}

      {/* Tooltip */}
      {tooltip && (
        <div
          className="absolute bg-black text-white text-xs rounded px-2 py-1"
          style={{ top: tooltip.y + 10, left: tooltip.x + 10 }}
        >
          {tooltip.text}
        </div>
      )}
    </section>
  );
}
